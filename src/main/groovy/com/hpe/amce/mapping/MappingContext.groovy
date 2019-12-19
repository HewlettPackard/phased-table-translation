package com.hpe.amce.mapping

import groovy.transform.Canonical
import groovy.transform.CompileStatic

/**
 * Contains mapping context.
 *
 * OO - Type of original object.
 * RO - Type of resulting object.
 * P - Type of parameters object.
 */
@Canonical
@CompileStatic
class MappingContext<OO, RO, P> {

    /**
     * Original object to be translated.
     */
    OO originalObject

    /**
     * Resulting object.
     */
    RO resultObject

    /**
     * Additional translator parameters passed from outside.
     */
    P parameters

}
