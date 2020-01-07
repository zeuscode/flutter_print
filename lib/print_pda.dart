import 'dart:async';
import 'package:flutter/services.dart';
import 'package:meta/meta.dart' show visibleForTesting;

class PrintPda {
  static PrintPda _instance;
  final EventChannel _eventChannel;
  final MethodChannel _methodChannel;
  Stream<String> _onPrintStatus;


  factory PrintPda() {
    if (_instance == null) {
      final EventChannel eventChannel =
          const EventChannel("plugins.flutter.io/missfresh.device_status");
      final MethodChannel methodChannel =
          const MethodChannel("plugins.flutter.io/missfresh.print");
      _instance = PrintPda.private(methodChannel, eventChannel);
    }
    return _instance;
  }

  @visibleForTesting
  PrintPda.private(this._methodChannel, this._eventChannel);

  Stream<String> get onScanResult {
    if (_onPrintStatus == null) {
      _onPrintStatus = _eventChannel
          .receiveBroadcastStream()
          .map((dynamic event) => event.toString());
    }
    return _onPrintStatus;
  }

  Future<String> print(String  text) async {
    return await _methodChannel.invokeMethod('print', {"text": text});
  }

  Future<String> get open => _methodChannel.invokeMapMethod("open",<String, dynamic>{
    'is_58mm':"true",
  }).then<String>((dynamic result) => result.toString());

  Future<String> init(bool is_58mm) async {
    return await _methodChannel.invokeMethod('init', {"is_58mm": is_58mm});
  }

}
