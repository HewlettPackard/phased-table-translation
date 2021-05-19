package com.hpe.amce.mapping.typical

import groovy.transform.CompileStatic

import java.time.Instant

@CompileStatic
class OtherClasses {

    @CompileStatic
    interface Exchange {

    }

    @CompileStatic
    interface ExchangeFormatter {

        String format(Exchange exchange)

    }

    @CompileStatic
    interface DistinguishedNameSerializer {

        String serialize(Map<String, ?> pairs)

    }

    @CompileStatic
    interface TimeSerializer {

        String serialize(Instant time)

    }

}
