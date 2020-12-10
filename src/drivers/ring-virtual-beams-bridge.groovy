/**
 *  Ring Virtual Beams Bridge Driver
 *
 *  Copyright 2019 Ben Rimmasch
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *  Change Log:
 *  2019-04-26: Initial
 *  2019-11-15: Import URL
 *  2020-02-29: Added checkin event
 *              Changed namespace
 *  2020-05-11: Fix error when receiving incomplete networks update msg
 *              Remove unnecessary safe object traversal
 */

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
  definition(name: "Ring Virtual Beams Bridge", namespace: "ring-hubitat-codahq", author: "Ben Rimmasch",
    importUrl: "https://raw.githubusercontent.com/codahq/ring_hubitat_codahq/master/src/drivers/ring-virtual-beams-bridge.groovy") {
    capability "Refresh"
    capability "Sensor"

    attribute "lastCheckin", "string"

    command "createDevices"
  }

  preferences {
    input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
  }
}

private logInfo(msg) {
  if (descriptionTextEnable) log.info msg
}

def logDebug(msg) {
  if (logEnable) log.debug msg
}

def logTrace(msg) {
  if (traceLogEnable) log.trace msg
}

def createDevices() {
  logDebug "Attempting to create devices."
  parent.createDevices(device.getDataValue("zid"))
}

def refresh() {
  logDebug "Attempting to refresh."
  parent.refresh(device.getDataValue("zid"))
}

def setValues(deviceInfo) {
  logDebug "updateDevice(deviceInfo)"
  logTrace "deviceInfo: ${JsonOutput.prettyPrint(JsonOutput.toJson(deviceInfo))}"

  if (deviceInfo.lastUpdate) {
    state.lastUpdate = deviceInfo.lastUpdate
  }
  if (deviceInfo.impulseType) {
    state.impulseType = deviceInfo.impulseType
  }
  if (deviceInfo.deviceType == "halo-stats.latency" && deviceInfo.state?.status == "success") {
    sendEvent(name: "lastCheckin", value: convertToLocalTimeString(new Date()), displayed: false, isStateChange: true)
  }
  if (deviceInfo.lastCommTime) {
    state.signalStrength = deviceInfo.lastCommTime
  }
  if (deviceInfo.state?.networks?.wlan0) {
    def nw = deviceInfo.state.networks

    if (nw.ppp0 != null) {
      if (nw.ppp0?.type) {
        device.updateDataValue("ppp0Type", nw.ppp0.type.capitalize())
      }
      if (nw.ppp0?.name) {
        device.updateDataValue("ppp0Name", nw.ppp0.name)
      }
      if (nw.ppp0?.rssi) {
        device.updateDataValue("ppp0Rssi", nw.ppp0.rssi.toString())
      }

      def type = device.getDataValue("ppp0Type")
      def name = device.getDataValue("ppp0Name")
      def rssi = device.getDataValue("ppp0Rssi")

      logInfo "ppp0 ${type} ${name} RSSI ${RSSI}"
      sendEvent(name: type, value: "${name} RSSI ${rssi}")
      state.ppp0 = "${name} RSSI ${rssi}"
    }

    if (nw.wlan0 != null) {
      if (nw.wlan0?.type) {
        device.updateDataValue("wlan0Type", nw.wlan0.type.capitalize())
      }
      if (nw.wlan0?.ssid) {
        device.updateDataValue("wlan0Ssid", nw.wlan0.ssid)
      }
      if (nw.wlan0?.rssi) {
        device.updateDataValue("wlan0Rssi", nw.wlan0.rssi.toString())
      }

      def type = device.getDataValue("wlan0Type")
      def ssid = device.getDataValue("wlan0Ssid")
      def rssi = device.getDataValue("wlan0Rssi")

      logInfo "ppp0 ${type} ${ssid} RSSI ${RSSI}"
      sendEvent(name: type, value: "${ssid} RSSI ${rssi}")
      state.wlan0 = "${ssid} RSSI ${rssi}"
    }
  }
  if (deviceInfo.deviceType == "adapter.ringnet" && deviceInfo.state?.version) {
    if (deviceInfo.state?.version?.nordicFirmwareVersion && device.getDataValue("nordicFirmwareVersion") != deviceInfo.state?.version?.nordicFirmwareVersion) {
      device.updateDataValue("nordicFirmwareVersion", deviceInfo.state?.version?.nordicFirmwareVersion)
    }
    if (deviceInfo.state?.version?.buildNumber && device.getDataValue("buildNumber") != deviceInfo.state?.version?.buildNumber) {
      device.updateDataValue("buildNumber", deviceInfo.state?.version?.buildNumber)
    }
    if (deviceInfo.state?.version?.softwareVersion && device.getDataValue("softwareVersion") != deviceInfo.state?.version?.softwareVersion) {
      device.updateDataValue("softwareVersion", deviceInfo.state?.version?.softwareVersion)
    }
  }
  else if (deviceInfo.state?.version) {
    if (device.getDataValue("version") != deviceInfo.state?.version) {
      device.updateDataValue("version", deviceInfo.state?.version.toString())
    }
  }
}

def checkChanged(attribute, newStatus) {
  if (device.currentValue(attribute) != newStatus) {
    logInfo "${attribute.capitalize()} for device ${device.label} is ${newStatus}"
    sendEvent(name: attribute, value: newStatus)
  }
}

private convertToLocalTimeString(dt) {
  def timeZoneId = location?.timeZone?.ID
  if (timeZoneId) {
    return dt.format("yyyy-MM-dd h:mm:ss a", TimeZone.getTimeZone(timeZoneId))
  }
  else {
    return "$dt"
  }
}