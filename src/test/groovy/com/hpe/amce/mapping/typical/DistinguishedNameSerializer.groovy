package com.hpe.amce.mapping.typical

import groovy.transform.CompileStatic

@CompileStatic
interface DistinguishedNameSerializer {

    String serialize(Map<String, ?> pairs)

}
