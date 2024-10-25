/********* DecibelPlugin.swift Cordova Plugin Implementation *******/

import Foundation

@objc(DecibelPlugin)
class DecibelPlugin : CDVPlugin {
    @objc(startListening:)func startListening(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate!.send(CDVPluginResult(status: CDVCommandStatus_OK), callbackId: command.callbackId)
    }

    @objc(stopListening:)func stopListening(_ command: CDVInvokedUrlCommand) {
        self.commandDelegate!.send(CDVPluginResult(status: CDVCommandStatus_OK), callbackId: command.callbackId)
    }
}