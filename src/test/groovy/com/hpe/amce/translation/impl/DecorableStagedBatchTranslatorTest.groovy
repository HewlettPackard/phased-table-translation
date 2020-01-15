package com.hpe.amce.translation.impl

import com.hpe.amce.translation.BatchTranslator
import spock.lang.Specification

class DecorableStagedBatchTranslatorTest extends Specification {

    def "aroundBatch controls what's done for batch"() {
        given:
        BatchTranslator aroundBatch = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            it.aroundBatch = aroundBatch
        }
        when:
        List result = instance.translateBatch(["input"], "context")
        then:
        1 * aroundBatch.translateBatch(["input"], "context") >> ["output"]
        result == ["output"]
    }

    def "aroundStage controls what's done for stage"() {
        given:
        Closure<List> stage = Mock()
        AroundStage aroundStage = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            it.aroundStage = aroundStage
            processingStages = [test: stage]
        }
        when:
        List result = instance.translateBatch(["input"], "context")
        then:
        1 * aroundStage.applyStage("test", stage, ["input"], "context") >> ["output"]
        0 * stage(*_)
        result == ["output"]
    }

    def "aroundElement controls what's done for element"() {
        given:
        Closure<List> stage = Mock()
        AroundElement aroundElement = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            it.aroundElement = aroundElement
            processingStages = [test: stage]
        }
        when:
        List result = instance.translateBatch(["input"], "context")
        then:
        1 * aroundElement.translateElement("test", stage, "input", "context") >> ["output"]
        0 * stage(*_)
        result == ["output"]
    }

    def "stages are applied sequentially by default"() {
        given:
        Closure<List> stage1 = Mock()
        Closure<List> stage2 = Mock()
        Closure<List> stage3 = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [c: stage1,
                                a: stage2,
                                b: stage3,
            ]
        }
        when:
        instance.translateBatch(["input"], "context")
        then:
        1 * stage1(*_) >> [1]
        then:
        1 * stage2(*_) >> [2]
        then:
        1 * stage3(*_) >> [3]
    }

    def "stage is sequentially applied to all elements of a batch"() {
        given:
        Closure<List> stage = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [test: stage]
        }
        when:
        instance.translateBatch([1, 2, 3], "context")
        then:
        1 * stage(1, _)
        then:
        1 * stage(2, _)
        then:
        1 * stage(3, _)
    }

    def "input of next stage is output of previous stage"() {
        given:
        Closure<List> stage1 = Mock()
        Closure<List> stage2 = Mock()
        Closure<List> stage3 = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [c: stage1,
                                a: stage2,
                                b: stage3,
            ]
        }
        when:
        instance.translateBatch([0], "context")
        then:
        1 * stage1(0, _) >> [1]
        1 * stage2(1, _) >> [2]
        1 * stage3(2, _) >> [3]
    }

    def "output is what last stage returns"() {
        given:
        Closure<List> stage1 = Mock()
        Closure<List> stage2 = Mock()
        Closure<List> stage3 = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [c: stage1,
                                a: stage2,
                                b: stage3,
            ]
        }
        when:
        List result = instance.translateBatch([0], "context")
        then:
        stage1(_, _) >> [1]
        stage2(_, _) >> [2]
        stage3(_, _) >> [3]
        result == [3]
    }

    def "by default element processing errors are ignored and output will just not contain result of processing erroneous element"() {
        given:
        Closure<List> stage = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [test: stage]
        }
        when:
        List result = instance.translateBatch([1, 2, 3], "context")
        then:
        stage(1, _) >> ["1"]
        stage(2, _) >> { throw new NullPointerException() }
        stage(3, _) >> ["3"]
        result == ["1", "3"]
    }

    def "stages can have different types of inputs"() {
        given:
        Closure<List<Tuple2<Integer, String>>> stage1 = { Integer input, String context ->
            [new Tuple2<Integer, String>(input, String.valueOf(input))]
        }
        Closure<List<String>> stage2 = { Tuple2<Integer, String> input, String context ->
            [input.second]
        }
        Closure<List<Integer>> stage3 = { String input, String context ->
            [Integer.parseInt(input)]
        }
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [c: stage1,
                                a: stage2,
                                b: stage3,
            ]
        }
        when:
        List result = instance.translateBatch([0], "context")
        then:
        result.first() == 0
    }

    def "translation context is passed to each stage"() {
        given:
        Object context = new Object()
        Closure<List> stage1 = Mock()
        Closure<List> stage2 = Mock()
        Closure<List> stage3 = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [c: stage1,
                                a: stage2,
                                b: stage3,
            ]
        }
        when:
        instance.translateBatch([0], context)
        then:
        stage1(_, context) >> [0]
        stage2(_, context) >> [0]
        stage3(_, context) >> [0]
    }

    def "can filter elements"() {
        given:
        Closure<List> stage1 = Mock()
        Closure<List> stage2 = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [c: stage1,
                                a: stage2,
            ]
        }
        when:
        List result = instance.translateBatch([1, 2, 3], "context")
        then:
        stage1(1, _) >> []
        stage1(2, _) >> [2]
        stage1(3, _) >> []
        stage2(2, _) >> ["2"]
        0 * stage2(*_)
        result == ["2"]
    }

    def "null means filter out"() {
        given:
        Closure<List> stage1 = Mock()
        Closure<List> stage2 = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [c: stage1,
                                a: stage2,
            ]
        }
        when:
        List result = instance.translateBatch([1, 2, 3], "context")
        then:
        stage1(1, _) >> null
        stage1(2, _) >> [2]
        stage1(3, _) >> null
        stage2(2, _) >> ["2"]
        0 * stage2(*_)
        result == ["2"]
    }

    def "can inject elements"() {
        given:
        Closure<List> stage1 = Mock()
        Closure<List> stage2 = Mock()
        Closure<List> stage3 = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [c: stage1,
                                a: stage2,
                                b: stage3,
            ]
        }
        when:
        List result = instance.translateBatch([0], "context")
        then:
        stage1(0, _) >> [1, 2]
        stage2(1, _) >> [11, 12]
        stage2(2, _) >> [21, 22]
        stage3(11, _) >> [111, 112]
        stage3(12, _) >> [211, 212]
        stage3(21, _) >> [311, 312]
        stage3(22, _) >> [411, 412]
        0 * _
        result == [111, 112, 211, 212, 311, 312, 411, 412]
    }

    def "can translate elements"() {
        given:
        Closure<List<Integer>> stage1 = { Integer input, String context ->
            [input + 1]
        }
        Closure<List<String>> stage2 = { Integer input, String context ->
            [input.toString()]
        }
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [c: stage1,
                                a: stage2,
            ]
        }
        when:
        List result = instance.translateBatch([10, 20, 30], "context")
        then:
        result == ["11", "21", "31"]
    }

    def "filter can drop all elements"() {
        given:
        Closure<List> stage1 = Mock()
        Closure<List> stage2 = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [c: stage1,
                                a: stage2,
            ]
        }
        when:
        List result = instance.translateBatch([1, 2, 3], "context")
        then:
        stage1(_, _) >> []
        0 * stage2(*_)
        result == []
    }

    def "returns empty list if input is null"() {
        given:
        Closure<List> stage = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [test: stage]
        }
        when:
        List result = instance.translateBatch(null, "context")
        then:
        0 * stage(_, _)
        result.empty
    }

    def "translation context can be null"() {
        given:
        Closure<List> stage = Mock()
        DecorableStagedBatchTranslator instance = new DecorableStagedBatchTranslator().tap {
            processingStages = [test: stage]
        }
        when:
        List result = instance.translateBatch(["input"], null)
        then:
        1 * stage("input", null) >> ["output"]
        result == ["output"]
    }

}
