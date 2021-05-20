package com.hpe.amce.mapping.typical

import groovy.transform.CompileStatic

@CompileStatic
interface ExchangeFormatter {

    String format(Exchange exchange)

}
