var exec = require('cordova/exec');

exports.startListening = function (decibelResult, error, pollingFrequency = 50) {
    exec(decibelResult, error, 'DecibelPlugin', 'startListening', [pollingFrequency]);
};

exports.stopListening = function (success, error) {
    exec(success, error, 'DecibelPlugin', 'stopListening', []);
};
