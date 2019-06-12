package com.hpe.amce.mapping

import com.hpe.amce.mapping.impl.MandatoryFieldMapper
import com.hpe.amce.mapping.impl.OptionalFieldMapper
import groovy.transform.Canonical
import groovy.transform.ToString
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.atomic.AtomicReference

class MapperTest extends Specification {

    static final String DATA_PATH = "src/test/groovy/" +
            MapperTest.name.replace('.', '/') + "Data.groovy"

    @Shared
    private ListAppender listAppender = LoggerContext.getContext(false).
            getRootLogger().appenders.get("LIST") as ListAppender

    void setup() {
        listAppender.clear()
    }

    /**
     * Field optionality.
     */
    enum Opt {
        /**
         * Field is mandatory.
         */
        M,
        /**
         * Field is optional.
         */
        O,
    }

    /**
     * Getter configuration.
     */
    enum Get {
        /**
         * Non-null value extracted.
         */
        Some,
        /**
         * Null value extracted.
         */
        Null,
        /**
         * Exception is thrown.
         */
        Ex,
        /**
         * Getter is not defined.
         */
        Undef,
    }

    /**
     * Validator configuration.
     */
    enum Val {
        /**
         * Value is ok.
         */
        Ok,
        /**
         * Value is not ok.
         */
        Bad,
        /**
         * Exception is thrown.
         */
        Ex,
        /**
         * Validator is not defined.
         */
        Undef,
    }

    /**
     * Defaulter configuration.
     */
    enum Def {
        /**
         * Non-null value is returned.
         */
        Some,
        /**
         * Null value is returned.
         */
        Null,
        /**
         * Exception is thrown.
         */
        Ex,
        /**
         * Default value is undefined.
         */
        Undef,
    }

    /**
     * Translator configuration.
     */
    enum Tr {
        /**
         * Non-null value is returned.
         */
        Some,
        /**
         * Null value is returned.
         */
        Null,
        /**
         * Exception is thrown.
         */
        Ex,
        /**
         * Translation is undefined.
         */
        Undef,
    }

    /**
     * Setter configuration.
     */
    enum Set {
        /**
         * Setter worked successfully.
         */
        Ok,
        /**
         * Exception is thrown.
         */
        Ex,
        /**
         * Setter (output field) is not defined.
         */
        Undef,
    }

    /**
     * Field result.
     */
    enum Fld {
        /**
         * Translated value was set.
         */
        Tr,
        /**
         * Default value was set.
         */
        Def,
        /**
         * Original value was set (as-is translation).
         */
        Orig,
        /**
         * Nothing was set.
         */
        None,
        /**
         * Data error has been raised.
         */
        DataErr,
        /**
         * Code error has been raised.
         */
        CodeErr,
    }

    /**
     * Log result.
     */
    enum Log {
        /**
         * Something was reported on warning level.
         */
        Warn,
        /**
         * Nothing was reported or it was reported on debug level.
         */
        None,
    }

    /**
     * Translator configuration.
     */
    @Canonical
    @ToString(includeNames = true, includePackage = false)
    static class Cfg {
        Opt optional
        Get getter
        Val validator
        Def defaulter
        Tr translator
        Set setter
    }

    /**
     * Translator behaviour.
     */
    @Canonical
    @ToString(includeNames = true, includePackage = false)
    static class Behaviuor {
        Fld fieldResult
        Log logResult
    }

    // For code assistance
    static void cfg(
            Opt optional,
            Get getter,
            Val validator,
            Def defaulter,
            Tr translator,
            Set setter,
            Fld desiredFieldResult,
            Log desiredLogResult) {
    }

    boolean isConfigError(Cfg cfg) {
        (cfg.getter == Get.Undef && cfg.defaulter == Def.Undef) ||
                (cfg.setter == Set.Undef &&
                        (cfg.validator == Val.Undef || cfg.defaulter != Def.Undef || cfg.translator != Tr.Undef))
    }

    boolean shouldDefaulterBeCalled(Cfg cfg) {
        if (isConfigError(cfg)) {
            return false
        }
        cfg.getter in [Get.Undef, Get.Ex, Get.Null] ||
                cfg.validator in [Val.Ex, Val.Bad] ||
                cfg.translator == Tr.Ex
    }

    boolean shouldTranslatorBeCalled(Cfg cfg) {
        if (isConfigError(cfg)) {
            return false
        }
        cfg.getter == Get.Some && cfg.validator in [Val.Undef, Val.Ok]
    }

    boolean shouldWarn(Cfg cfg) {
        if (isConfigError(cfg)) {
            return false
        }
        if (cfg.optional == Opt.M && cfg.defaulter != Def.Undef) {
            cfg.getter in [Get.Ex, Get.Null] ||
                    (cfg.getter == Get.Some && (cfg.validator in [Val.Ex, Val.Bad] || cfg.translator == Tr.Ex))
        } else if (cfg.optional == Opt.O) {
            cfg.getter in [Get.Ex] ||
                    (cfg.getter == Get.Some && (cfg.validator in [Val.Ex, Val.Bad] || cfg.translator == Tr.Ex))
        } else {
            false
        }
    }

    boolean shouldSetterBeCalled(Cfg cfg) {
        if (isConfigError(cfg)) {
            return false
        }
        (cfg.getter == Get.Some &&
                cfg.validator in [Val.Undef, Val.Ok] &&
                (cfg.translator in [Tr.Some, Tr.Undef] || (cfg.optional == Opt.O && cfg.translator == Tr.Null))) ||
                cfg.defaulter == Def.Some
    }

    Log getActualLogResult() {
        listAppender.events.any {
            it.loggerName.startsWith(getClass().package.name) &&
                    it.level == Level.WARN
        } ? Log.Warn : Log.None
    }

    @Unroll
    def "generate wanted behavior table to #file"() {
        when:
        new File(file).withPrintWriter { writer ->
            writer.println "package ${MapperTest.getPackage().name}"
            [Opt, Get, Val, Def, Tr, Set, Fld, Log].each {
                writer.println "import ${it.name.replace(/$/, '.')} as ${it.simpleName}"
            }
            writer.println "import static ${MapperTest.name}.cfg"
            writer.println "class ${MapperTest.simpleName}Data {\n"
            int index = 1
            Opt.values().each { optional ->
                Get.values().each { getter ->
                    Val.values().each { validator ->
                        Def.values().each { defaulter ->
                            Tr.values().each { translator ->
                                Set.values().each { setter ->
                                    Cfg cfg = new Cfg(optional, getter, validator, defaulter, translator, setter)
                                    Fld field
                                    Log log
                                    if (getter == Get.Undef && defaulter == Def.Undef) {
                                        // Neither getter, nor defaulter are set.
                                        // What will I translate?
                                        field = Fld.CodeErr
                                    } else if (setter == Set.Undef && (
                                            validator == Val.Undef ||
                                                    defaulter != Def.Undef ||
                                                    translator != Tr.Undef)) {
                                        // If setter is not set then we are in pure checking mode.
                                        // This is pointless if validator is not set.
                                        // Also defaulter and translation are useless in this case.
                                        // To avoid confusing configuration, let's deny it.
                                        field = Fld.CodeErr
                                    } else if (getter == Get.Undef && (
                                            validator in [Val.Ok, Val.Bad, Val.Ex] ||
                                                    translator in [Tr.Some, Tr.Null, Tr.Ex])) {
                                        // If we have no getter then it's pointless to set validator or translator
                                        // because they won't be called anyway.
                                        field = Fld.CodeErr
                                    } else if ((setter == Set.Ex && shouldSetterBeCalled(cfg)) ||
                                            (defaulter == Def.Ex && shouldDefaulterBeCalled(cfg))) {
                                        // Setter deals with translated values -> should not throw.
                                        // Defaulter must not throw by definition.
                                        // But we gona know about defaulter only if we had a chance to call it.
                                        field = Fld.CodeErr
                                    } else if (optional == Opt.M &&
                                            translator == Tr.Null &&
                                            shouldTranslatorBeCalled(cfg)) {
                                        // If field is mandatory then why translator returns null?
                                        field = Fld.CodeErr
                                    } else if (defaulter == Def.Null && shouldDefaulterBeCalled(cfg)) {
                                        // Defaulter returning null makes no sense.
                                        field = Fld.CodeErr
                                    } else if (optional == Opt.M &&
                                            defaulter == Def.Undef && (
                                            getter in [Get.Null, Get.Ex] ||
                                                    validator in [Val.Bad, Val.Ex] ||
                                                    translator == Tr.Ex)) {
                                        // If defaulter is not set for mandatory field and
                                        // input is absent, is invalid or we can't translate it
                                        // then the input is bad.
                                        field = Fld.DataErr
                                    } else if (optional == Opt.O &&
                                            defaulter == Def.Undef && (
                                            getter in [Get.Null, Get.Ex] ||
                                                    validator in [Val.Bad, Val.Ex] ||
                                                    translator == Tr.Ex)) {
                                        // If defaulter is not set for optional field and
                                        // input is absent, is invalid or we can't translate it
                                        // then we just skip the field.
                                        field = Fld.None
                                    } else if (shouldDefaulterBeCalled(cfg) && setter == Set.Ok) {
                                        // Default value is used if anything bad happens.
                                        field = Fld.Def
                                    } else if (getter == Get.Some &&
                                            validator in [Val.Undef, Val.Ok] &&
                                            translator in [Tr.Some, Tr.Null] &&
                                            setter == Set.Ok) {
                                        // If everything is ok then translated value is used.
                                        field = Fld.Tr
                                    } else if (getter == Get.Some &&
                                            validator in [Val.Undef, Val.Ok] &&
                                            translator == Tr.Undef &&
                                            setter == Set.Ok) {
                                        // Original value is used if everything is ok but translator is not set.
                                        // This is when we copy value as is.
                                        field = Fld.Orig
                                    } else if (getter == Get.Some && validator == Val.Ok && setter == Set.Undef) {
                                        // Pure validation success scenario.
                                        field = Fld.None
                                    } else if (getter == Get.Undef &&
                                            validator == Val.Undef &&
                                            translator == Tr.Undef &&
                                            defaulter in [Def.Some, Def.Null] &&
                                            setter == Set.Ok) {
                                        // Always set a field to a constant value when no input field.
                                        field = Fld.Def
                                    } else {
                                        throw new IllegalStateException("Don't know expected result for $cfg")
                                    }
                                    log = shouldWarn(cfg) ? Log.Warn : Log.None
                                    writer.println "def l${index++}(){cfg(" +
                                            "Opt.${optional}" +
                                            ", Get.${getter}" +
                                            ", Val.${validator}" +
                                            ", Def.${defaulter}" +
                                            ", Tr.${translator}" +
                                            ", Set.${setter}" +
                                            ", Fld.${field}" +
                                            ", Log.${log}" +
                                            ")}"
                                }
                            }
                        }
                    }
                }
            }
            writer.println "\n}"
        }

        then:
        true

        where:
        file      | _
        DATA_PATH | _
    }

    @Unroll
    def "auto for #loadedConfig should be #loadedDesiredBehaviour"() {
        given:
        Cfg config = loadedConfig as Cfg
        Behaviuor desiredBehaviour = loadedDesiredBehaviour as Behaviuor
        AtomicReference<Fld> fieldResult = new AtomicReference<>(Fld.None)
        MappingContext mappingContext =
                new MappingContext("original", "result", ["param1", "param2"])
        Field mapper = cfgToMapper(config, fieldResult)

        when:
        try {
            config.optional == Opt.M ?
                    new MandatoryFieldMapper<>().mapField(mapper, mappingContext) :
                    new OptionalFieldMapper<>().mapField(mapper, mappingContext)
        } catch (IllegalArgumentException e) {
            fieldResult.set(Fld.DataErr)
        } catch (IllegalStateException e) {
            fieldResult.set(Fld.CodeErr)
        }

        then:
        fieldResult.get() == desiredBehaviour.fieldResult
        actualLogResult == desiredBehaviour.logResult

        where:
        [loadedConfig, loadedDesiredBehaviour] << loadTable(DATA_PATH)
    }

    @Unroll
    def "manual for #config should be #desiredBehaviour"() {
        given:
        AtomicReference<Fld> fieldResult = new AtomicReference<>(Fld.None)
        MappingContext mappingContext =
                new MappingContext("original object", "result object", ["param1", "param2"])
        Field mapper = cfgToMapper(config, fieldResult)

        when:
        try {
            config.optional == Opt.M ?
                    new MandatoryFieldMapper<>().mapField(mapper, mappingContext) :
                    new OptionalFieldMapper<>().mapField(mapper, mappingContext)
        } catch (IllegalArgumentException e) {
            fieldResult.set(Fld.DataErr)
        } catch (IllegalStateException e) {
            fieldResult.set(Fld.CodeErr)
        }

        then:
        fieldResult.get() == desiredBehaviour.fieldResult
        actualLogResult == desiredBehaviour.logResult

        where:
        config                                                       | desiredBehaviour
        new Cfg(Opt.M, Get.Some, Val.Bad, Def.Some, Tr.Some, Set.Ok) | new Behaviuor(Fld.Def, Log.Warn)
    }

    def "closures have access to context and parameters"() {
        given:
        boolean getterOk = false
        boolean validatorOk = false
        boolean translatorOk = false
        boolean setterOk = false
        boolean defaulterOk = false
        MappingContext<Map<String, String>, Map<String, String>, List<String>> mappingContext = new MappingContext<>(
                originalObject: [a: "b"],
                resultObject: ["x": "y"],
                parameters: ["param"])
        Field<Map<String, String>, Map<String, String>, String, String, List<String>> field = new Field<>()
        when:
        field.tap {
            withId("testField")
            withGetter {
                assert parameters[0] == "param"
                assert originalObject["a"] == "b"
                assert resultObject["x"] == "y"
                assert it
                assert it.containsKey("a")
                getterOk = true
                it["a"]
            }
            withValidator {
                assert parameters[0] == "param"
                assert originalObject["a"] == "b"
                assert resultObject["x"] == "y"
                assert it == "b"
                validatorOk = true
                true
            }
            withTranslator {
                assert parameters[0] == "param"
                assert originalObject["a"] == "b"
                assert resultObject["x"] == "y"
                assert it == "b"
                if (!translatorOk) {
                    translatorOk = true
                    throw new NullPointerException("Let's branch to defaulter")
                } else {
                    "c" // Just to avoid lots of idea highlighting
                }
            }
            withDefaulter {
                assert parameters[0] == "param"
                assert originalObject["a"] == "b"
                assert resultObject["x"] == "y"
                defaulterOk = true
                "d"
            }
            withSetter {
                assert parameters[0] == "param"
                assert originalObject["a"] == "b"
                assert resultObject["x"] == "y"
                assert it == "d"
                resultObject["a"] = it
                setterOk = true
            }
        }
        new OptionalFieldMapper<>().mapField(field, mappingContext)
        then:
        getterOk
        validatorOk
        translatorOk
        setterOk
        defaulterOk
        mappingContext.resultObject["a"] == "d"
    }

    private static List<List<?>> loadTable(String path) {
        List<String> lines = new File(path).readLines()
        lines = lines.findAll { it =~ /\s*def.+cfg\(.+\)/ }
        lines = lines.collect { it.replaceAll(/def l\d+\(\)\{cfg\(/, '') }
        lines = lines.collect { it.replaceAll(/\)}/, '') }
        lines = lines.collect { it.replaceAll(/\w+\.(\w+)/, /$1/) }
        List<List<String>> records = lines.collect { it.split(/,/)*.trim() }
        records.collect {
            int i = 0
            [
                    new Cfg(
                            Opt.valueOf(it[i++]),
                            Get.valueOf(it[i++]),
                            Val.valueOf(it[i++]),
                            Def.valueOf(it[i++]),
                            Tr.valueOf(it[i++]),
                            Set.valueOf(it[i++])
                    ),
                    new Behaviuor(
                            Fld.valueOf(it[i++]),
                            Log.valueOf(it[i++])
                    ),
            ]
        }
    }

    private static cfgToMapper(Cfg config, AtomicReference<Fld> fieldResult) {
        new Field().tap {
            withId("$config")
            switch (config.getter) {
                case Get.Some:
                    withGetter { "original" }
                    break
                case Get.Null:
                    withGetter { null }
                    break
                case Get.Ex:
                    withGetter { throw new NullPointerException("ex from getter") }
                    break
                case Get.Undef:
                    break
            }
            switch (config.validator) {
                case Val.Ok:
                    withValidator { true }
                    break
                case Val.Bad:
                    withValidator { false }
                    break
                case Val.Ex:
                    withValidator { throw new NullPointerException("ex from validator") }
                    break
                case Val.Undef:
                    break
            }
            switch (config.defaulter) {
                case Def.Some:
                    withDefaulter { "default" }
                    break
                case Def.Null:
                    withDefaulter { null }
                    break
                case Def.Ex:
                    withDefaulter { throw new NullPointerException("ex from defaulter") }
                    break
                case Def.Undef:
                    break
            }
            switch (config.translator) {
                case Tr.Some:
                    withTranslator { "translated" }
                    break
                case Tr.Null:
                    withTranslator { null }
                    break
                case Tr.Ex:
                    withTranslator { throw new NullPointerException("ex from translator") }
                    break
                case Tr.Undef:
                    break
            }
            switch (config.setter) {
                case Set.Ok:
                    withSetter {
                        switch (it) {
                            case "default":
                                fieldResult.set(Fld.Def)
                                break
                            case "translated":
                                fieldResult.set(Fld.Tr)
                                break
                            case "original":
                                fieldResult.set(Fld.Orig)
                                break
                            case null:
                                // If success case and translator returned null then it's translated result.
                                // If input data not ok and default set to null then it's default result.
                                if (config.getter == Get.Some &&
                                        config.validator in [Val.Undef, Val.Ok] &&
                                        config.translator == Tr.Null) {
                                    fieldResult.set(Fld.Tr)
                                } else if ((config.getter in [Get.Null, Get.Ex] ||
                                        config.validator in [Val.Ex, Val.Bad]) && config.defaulter == Def.Null) {
                                    fieldResult.set(Fld.Def)
                                } else {
                                    throw new IllegalStateException("Don't know how to react on $config")
                                }
                                break
                            default:
                                throw new IllegalStateException("Setter received unexpected value: $it")
                        }
                    }
                    break
                case Set.Ex:
                    withSetter { throw new NullPointerException("ex from setter") }
                    break
                case Set.Undef:
                    break
            }
        }
    }

}
