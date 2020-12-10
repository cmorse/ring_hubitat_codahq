/**
 *  Ring Virtual Beams Motion Sensor Driver
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
 *  2019-11-24: Fix for motion events not working... the entire point of this device.
 *  2020-02-12: Fixed battery % to show correctly in dashboards
 *  2020-02-29: Added checkin event
 *              Changed namespace
 *
 */

import groovy.json.JsonSlurper

metadata {
  definition(name: "Ring Virtual Beams Motion Sensor", namespace: "ring-hubitat-codahq", author: "Ben Rimmasch",
    importUrl: "https://raw.githubusercontent.com/codahq/ring_hubitat_codahq/master/src/drivers/ring-virtual-beams-motion-sensor.groovy") {
    capability "Refresh"
    capability "Sensor"
    capability "Motion Sensor"
    capability "Battery"
    capability "TamperAlert"

    attribute "lastCheckin", "string"
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

def refresh() {
  logDebug "Attempting to refresh."
  //parent.simpleRequest("refresh-device", [dni: device.deviceNetworkId])
}

def setValues(deviceInfo) {
  logDebug "updateDevice(deviceInfo)"
  logTrace "deviceInfo: ${deviceInfo}"

  if (deviceInfo.state && deviceInfo.state.motionStatus != null) {
    def motion = deviceInfo.state.motionStatus == "clear" ? "inactive" : "active"
    checkChanged("motion", motion)
  }
  if (deviceInfo.batteryLevel != null) {
    checkChanged("battery", deviceInfo.batteryLevel, "%")
  }
  if (deviceInfo.tamperStatus) {
    def tamper = deviceInfo.tamperStatus == "tamper" ? "detected" : "clear"
    checkChanged("tamper", tamper)
  }
  if (deviceInfo.lastUpdate != state.lastUpdate) {
    state.lastUpdate = deviceInfo.lastUpdate
    sendEvent(name: "lastCheckin", value: convertToLocalTimeString(new Date()), displayed: false, isStateChange: true)
  }
  if (deviceInfo.impulseType) {
    state.impulseType = deviceInfo.impulseType
  }
  if (deviceInfo.lastCommTime) {
    state.signalStrength = deviceInfo.lastCommTime
  }
  if (deviceInfo.nextExpectedWakeup) {
    state.nextExpectedWakeup = deviceInfo.nextExpectedWakeup
  }
  if (deviceInfo.signalStrength) {
    state.signalStrength = deviceInfo.signalStrength
  }
  if (deviceInfo.firmware && device.getDataValue("firmware") != deviceInfo.firmware) {
    device.updateDataValue("firmware", deviceInfo.firmware)
  }
  if (deviceInfo.hardwareVersion && deviceInfo.hardwareVersion != "null" && device.getDataValue("hardwareVersion") != deviceInfo.hardwareVersion) {
    device.updateDataValue("hardwareVersion", deviceInfo.hardwareVersion)
  }
}

def checkChanged(attribute, newStatus) {
  checkChanged(attribute, newStatus, null)
}

def checkChanged(attribute, newStatus, unit) {
  if (device.currentValue(attribute) != newStatus) {
    logInfo "${attribute.capitalize()} for device ${device.label} is ${newStatus}"
    sendEvent(name: attribute, value: newStatus, unit: unit)
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
