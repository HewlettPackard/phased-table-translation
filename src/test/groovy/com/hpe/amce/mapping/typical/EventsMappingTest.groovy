package com.hpe.amce.mapping.typical

import com.hpe.amce.mapping.impl.SimpleObjectMapper
import groovy.json.JsonSlurper
import spock.lang.Specification

import java.time.Instant
import java.time.format.DateTimeFormatter

class EventsMappingTest extends Specification {

    Map<String, ?> rawEvent

    Exchange exchange = Mock()

    ExchangeFormatter exchangeFormatter = Mock()

    EventsMapping.BatchContext batchContext = new EventsMapping.BatchContext(
            exchangeFormatter,
            exchange)

    private Map<String, ?> parseRaw(String raw) {
        (Map) new JsonSlurper().parseText(raw)
    }

    EventsMapping eventsMapping = new EventsMapping().tap { mapping ->
        mapping.configuredAgentEntity = 'configuredAgentEntity'
        mapping.probableCauses = [
                'a-bis to bts interface failure': 'a-bis to bts interface failure',
                'a-bis to trx interface failure': 'a-bis to trx interface failure',
                'adapter error'                 : 'adapter error',
                'air compressor failure'        : 'air compressor failure',
                'fire'                          : 'fire',
                'fire detector failure'         : 'fire detector failure',
        ]
        mapping.alarmTypes = [
                CommunicationsAlarm  : 'CommunicationsAlarm',
                ProcessingErrorAlarm : 'ProcessingErrorAlarm',
                EnvironmentalAlarm   : 'EnvironmentalAlarm',
                QualityOfServiceAlarm: 'QualityOfServiceAlarm',
                EquipmentAlarm       : 'EquipmentAlarm',
        ]
        mapping.severities = [
                Cleared      : 'Cleared',
                Indeterminate: 'Indeterminate',
                Critical     : 'Critical',
                Major        : 'Major',
                Minor        : 'Minor',
                Warning      : 'Warning',
        ]
        mapping.mapper = new SimpleObjectMapper<>()
        mapping.timeSerializer = Mock(TimeSerializer)
        mapping.distinguishedNameSerializer = Mock(DistinguishedNameSerializer)
    }

    def 'notifyNewAlarm'() {
        given:
        rawEvent = parseRaw """
{
  "in_notificationType": "notifyNewAlarm",
  "in_alarmType": "CommunicationsAlarm",
  "in_objectClass": "PHYSICAL_TERMINATION_POINT",
  "in_objectInstance": "IRPNetwork=ABCNetwork,Subnet=TN2,BSS=B5C0100",
  "in_notificationId": 123,
  "in_correlatedNotifications": [
    1,
    2
  ],
  "in_eventTime": "1937-01-01T12:00:27.87+00:20",
  "in_systemDN": "DC=www.some_example.org, SubNetwork=1, ManagementNode=1, IRPAgent=1",
  "in_alarmId": "ABC:5654",
  "in_agentEntity": "ems_south",
  "in_probableCause": "fire",
  "in_perceivedSeverity": "Critical",
  "in_specificProblem": [
    "example specific problem 1"
  ],
  "in_additionalText": "Everything is on fire!",
  "in_siteLocation": "Lindau",
  "in_regionLocation": "Bavaria",
  "in_vendorName": "Some Company",
  "in_technologyDomain": "Mobile",
  "in_equipmentModel": "MNM 3000",
  "in_plannedOutageIndication": false,
  "in_customStringAttribute": "custom string value",
  "in_customListAttribute": [
    "custom value 1",
    "custom value 2",
    "custom value 3"
  ]
}
"""
        when:
        List<Map<String, Object>> result = eventsMapping.mapEvent(rawEvent, batchContext)
        then:
        1 * eventsMapping.timeSerializer.serialize(_) >> { Instant parsedTime -> parsedTime.toString() }
        0 * eventsMapping.distinguishedNameSerializer.serialize(_) >> { Map<String, ?> dn ->
            dn.collect { k, v -> "$k=$v" }.join(',')
        }
        result.size() == 1
        Map<String, Object> event = result.first()
        event['out_notificationType'] == 'notifyNewAlarm'
        event['out_alarmType'] == 'CommunicationsAlarm'
        event['out_objectClass'] == 'PHYSICAL_TERMINATION_POINT'
        event['out_objectInstance'] == "IRPNetwork=ABCNetwork,Subnet=TN2,BSS=B5C0100"
        event['out_notificationId'] == 123L
        event['out_correlatedNotifications'] == [1L, 2L]
        Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(event['out_eventTime'] as String)) ==
                Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("1937-01-01T12:00:27.87+00:20"))
        event['out_systemDN'] == "DC=www.some_example.org, SubNetwork=1, ManagementNode=1, IRPAgent=1"
        event['out_alarmId'] == "ABC:5654"
        event['out_agentEntity'] == 'configuredAgentEntity'
        event['out_probableCause'] == 'fire'
        event['out_perceivedSeverity'] == 'Critical'
        event['out_specificProblem'] == ["example specific problem 1"]
        event['out_additionalText'] == "Everything is on fire!"
        event['out_siteLocation'] == 'Lindau'
        event['out_regionLocation'] == 'Bavaria'
        event['out_vendorName'] == "Some Company"
        event['out_technologyDomain'] == 'Mobile'
        event['out_equipmentModel'] == "MNM 3000"
        event['out_plannedOutageIndication'] == Boolean.FALSE
        event['out_proposedRepairActions'] == null
        event['out_ackTime'] == null
        event['out_ackUserId'] == null
        event['out_ackState'] == null
        event['out_comments'] == null
    }

}
