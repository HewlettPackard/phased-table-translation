package com.hpe.amce.mapping.typical

import groovy.transform.CompileStatic

import java.time.Instant

@CompileStatic
interface TimeSerializer {

    String serialize(Instant time)

}
