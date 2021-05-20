package com.hpe.amce.mapping.typical

import com.hpe.amce.mapping.Field
import com.hpe.amce.mapping.ObjectMapper
import groovy.transform.CompileStatic
import groovy.transform.ToString

import javax.annotation.Nonnull
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Translates notifications.
 */
@CompileStatic
class EventsMapping {

    /**
     * Mapping table for raise alarm notifications.
     */
    Map<Field<Map<String, Object>, Map<String, Object>, ?, ?, EventContext>, Boolean> notifyNewAlarm = [
            (new F<Void, String>()
                    .withId('notificationType')
                    .withDefaulter { 'notifyNewAlarm' }
                    .withSetter { resultObject['out_notificationType'] = it })       : true,
            (new F<Void, String>()
                    .withId('agentEntity')
                    .withDefaulter { configuredAgentEntity }
                    .withSetter { resultObject['out_agentEntity'] = it })            : true,
            (new F<String, String>()
                    .withId('alarmId')
                    .withGetter { it['in_alarmId'] as String }
                    .withValidator(Closure.IDENTITY)
                    .withTranslator { it }
                    .withSetter { resultObject['out_alarmId'] = it })                : true,
            (new F<String, String>()
                    .withId('alarmType')
                    .withGetter { it['in_alarmType'] as String }
                    .withValidator { alarmTypes.containsKey(it) }
                    .withTranslator { alarmTypes[it] }
                    .withSetter { resultObject['out_alarmType'] = it })              : true,
            (new F<String, String>()
                    .withId('objectClass')
                    .withGetter { it['in_objectClass'] as String }
                    .withSetter { resultObject['out_objectClass'] = it })            : false,
            (new F<Map<String, Object>, String>()
                    .withId('objectInstance')
                    .withGetter { it.findAll { it.key in rtObjectInstanceFields } }
                    .withValidator { (it['in)CLASS_2'] && it['in_CLASS_1']) || it['in_objectInstance'] }
                    .withTranslator {
                        (it['in_objectInstance'] as String) ?:
                                distinguishedNameSerializer.serialize([
                                        CLASS_1: it['in_CLASS_1'],
                                        CLASS_2: it['in_CLASS_2'],
                                ])
                    }
                    .withSetter { resultObject['out_objectInstance'] = it })         : true,
            (new F<Long, Long>()
                    .withId('notificationId')
                    .withGetter { it['in_notificationId'] as Long }
                    .withSetter { resultObject['out_notificationId'] = it })         : false,
            (new F<List<Long>, List<Long>>()
                    .withId('correlatedNotifications')
                    .withGetter { List.cast(it['in_correlatedNotifications'])*.asType(Long) ?: null }
                    .withValidator(Closure.IDENTITY)
                    .withSetter { resultObject['out_correlatedNotifications'] = it }): false,
            (new F<String, String>()
                    .withId('eventTime')
                    .withGetter { it['in_eventTime'] as String }
                    .withValidator(Closure.IDENTITY)
                    .withDefaulter { timeSerializer.serialize(Instant.now()) }
                    .withTranslator { timeSerializer.serialize(OffsetDateTime.parse(it).toInstant()) }
                    .withSetter { resultObject['out_eventTime'] = it })              : true,
            (new F<String, String>()
                    .withId('systemDN')
                    .withGetter { it['in_systemDN'] as String }
                    .withSetter { resultObject['out_systemDN'] = it })               : false,
            (new F<String, String>()
                    .withId('probableCause')
                    .withGetter { it['in_probableCause'] as String }
                    .withValidator { probableCauses.containsKey(it) }
                    .withDefaulter { 'indeterminate' }
                    .withTranslator { probableCauses[it] }
                    .withSetter { resultObject['out_probableCause'] = it })          : true,
            (new F<String, String>()
                    .withId('perceivedSeverity')
                    .withGetter { it['in_perceivedSeverity'] as String }
                    .withValidator { severities.containsKey(it) }
                    .withDefaulter { 'Indeterminate' }
                    .withTranslator { severities[it] }
                    .withSetter { resultObject['out_perceivedSeverity'] = it })      : true,
            (new F<List<String>, List<String>>()
                    .withId('specificProblem')
                    .withGetter { List.cast(it['in_specificProblem'])?.collect { it as String } }
                    .withSetter { resultObject['out_specificProblem'] = it })        : false,
            (new F<String, String>()
                    .withId('additionalText')
                    .withGetter { it['in_additionalText'] as String }
                    .withSetter { resultObject['out_additionalText'] = it })         : false,
            (new F<String, String>()
                    .withId('siteLocation')
                    .withGetter { it['in_siteLocation'] as String }
                    .withSetter { resultObject['out_siteLocation'] = it })           : false,
            (new F<String, String>()
                    .withId('regionLocation')
                    .withGetter { it['in_regionLocation'] as String }
                    .withSetter { resultObject['out_regionLocation'] = it })         : false,
            (new F<String, String>()
                    .withId('vendorName')
                    .withGetter { it['in_vendorName'] as String }
                    .withSetter { resultObject['out_vendorName'] = it })             : false,
            (new F<String, String>()
                    .withId('technologyDomain')
                    .withGetter { it['in_technologyDomain'] as String }
                    .withSetter { resultObject['out_technologyDomain'] = it })       : false,
            (new F<String, String>()
                    .withId('equipmentModel')
                    .withGetter { it['in_equipmentModel'] as String }
                    .withSetter { resultObject['out_equipmentModel'] = it })         : false,
            (new F<Boolean, Boolean>()
                    .withId('plannedOutageIndication')
                    .withGetter { it['in_plannedOutageIndication'] as Boolean }
                    .withSetter { resultObject['out_plannedOutageIndication'] = it }): false,
    ]

    /**
     * Value of agentEntity specified in configuration.
     *
     * Remove this when coding for real EMS and it provides agentEntity.
     * Otherwise, keep the parameter but remove this comment.
     */
    @Nonnull
    String configuredAgentEntity

    /**
     * Mapping between probable causes sent by EMS and standard TMB probable causes.
     */
    @Nonnull
    Map<String, String> probableCauses

    /**
     * Mapping between alarm types sent by EMS and standard TMB alarm types.
     */
    @Nonnull
    Map<String, String> alarmTypes

    /**
     * Mapping between severities sent by EMS and standard TMB severities.
     */
    @Nonnull
    Map<String, String> severities

    /**
     * Fields used to translate objectInstance in real time notifications.
     */
    @Nonnull
    final Set<String> rtObjectInstanceFields = ['in_CLASS_1', 'in_CLASS_2', 'in_objectInstance'] as Set

    /**
     * Serializes distinguished name that will be sent to the bus.
     */
    @Nonnull
    DistinguishedNameSerializer distinguishedNameSerializer

    /**
     * Converter for dates.
     */
    @Nonnull
    TimeSerializer timeSerializer

    /**
     * Mapper to be used to map events.
     */
    @Nonnull
    ObjectMapper<Map<String, Object>, Map<String, Object>, EventContext> mapper

    /**
     * Maps single real time event.
     *
     * @param raw Incoming event.
     * @param batchContext Mapping context.
     * @return List of outgoing mapped events.
     */
    List<Map<String, Object>> mapEvent(Map<String, Object> raw, BatchContext batchContext) {
        String notificationType = 'notifyNewAlarm'
        Map<String, Object> event = [raw: Object.cast(raw)]
        EventContext eventContext = new EventContext(batchContext, notificationType)
        mapper.mapAllFields(raw, event, notifyNewAlarm, eventContext)
        List<Map<String, Object>> result = Collections.singletonList(Map.cast(event))
        result
    }

    /**
     * A field with pre-set types of original and result objects for real time and resync channels.
     * OF - Type of original field.
     * RF - Type of result field.
     */
    static class F<OF, RF> extends Field<Map<String, Object>, Map<String, Object>, OF, RF, EventContext> {
    }

    /**
     * Custom context that caches data from field to field while translating an event.
     */
    @ToString(includeNames = true, includePackage = false, ignoreNulls = false)
    static class EventContext {

        /**
         * Context of a batch.
         */
        BatchContext batchContext

        /**
         * Name of the mapping table being used to map fields of event.
         */
        String mappingTableName

        /**
         * Creates new instance.
         * @param batchContext Context of a batch.
         * @param mappingTableName Name of the mapping table being used to map the event.
         */
        EventContext(@Nonnull BatchContext batchContext, @Nonnull String mappingTableName) {
            this.batchContext = batchContext
            this.mappingTableName = mappingTableName
        }
    }

    /**
     * Context of batch translation.
     */
    static class BatchContext {

        final Exchange exchange

        final ExchangeFormatter exchangeFormatter

        /**
         * Creates new instance.
         * @param exchangeFormatter Formatter used to dump exchange that is being processed.
         * @param exchange Exchange that is being processed.
         */
        BatchContext(@Nonnull ExchangeFormatter exchangeFormatter,
                     @Nonnull Exchange exchange) {
            this.exchange = exchange
            this.exchangeFormatter = exchangeFormatter
        }

        @Override
        String toString() {
            return """{exchange=${exchangeFormatter.format(exchange)}}"""
        }

    }

}
