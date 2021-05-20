package com.hpe.amce.mapping

import com.hpe.amce.mapping.impl.SimpleObjectMapper
import com.hpe.amce.mapping.typical.Exchange
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.ToString
import spock.lang.Specification

import javax.annotation.Nonnull
import java.nio.charset.StandardCharsets

@CompileStatic
class Groovy307CompileStaticTest extends Specification {

    @Nonnull
    String charset = StandardCharsets.UTF_8.name()

    @Nonnull
    Map<Field<ReplyEms, ReplyBus, ?, ?, Exchange>, Boolean> mapping = [
            (new F<Void, String>()
                    .withId('CONTENT_TYPE')
                    .withDefaulter { charset }
                    .withSetter {
                        parameters.setExchangeProperty('charset', charset)
                        // The following form does not work in Groovy 3.0.7
                        // resultObject.headers['Content-Type']=
                        //         "application/json; charset=$charset".toString()
                        // However, this one works
                        resultObject.headers.put('Content-Type',
                                "application/json; charset=$charset".toString())
                    }): true,
    ]

    @ToString(includeNames = true, includePackage = false)
    private static class ReplyEms {
        @Nonnull
        Map<String, Object> headers
        @Nonnull
        Map<String, Object> body

        ReplyEms(@Nonnull Exchange exchange) {
            headers = [:]
            body = [:]
        }
    }

    @ToString(includeNames = true, includePackage = false)
    private static class ReplyBus {
        @Nonnull
        Map<String, Object> headers = [:]
        @Nonnull
        Map<String, Object> body = [:]
    }

    /**
     * A field with pre-set types of original and result objects for reply mapping.
     * OF - Type of original field.
     * RF - Type of result field.
     */
    private static class F<OF, RF> extends Field<ReplyEms, ReplyBus, OF, RF, Exchange> {
    }

    @CompileDynamic
    def 'mapping works'() {
        given:
        Exchange exchange = Mock()
        ObjectMapper<ReplyEms, ReplyBus, Exchange> mapper = new SimpleObjectMapper<>()
        ReplyEms input = new ReplyEms(exchange)
        ReplyBus output = new ReplyBus()
        when:
        mapper.mapAllFields(input, output, mapping, exchange)
        then:
        1 * exchange.setExchangeProperty('charset', charset)
        output.headers == ['Content-Type': 'application/json; charset=UTF-8' as Object]
    }

}
