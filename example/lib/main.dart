import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:print_pda/print_pda.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  var _print;

  @override
  void initState() {
    super.initState();
    if(mounted) {
      _print = PrintPda();
      _print.onScanResult.listen((String result){
        print("返回结果:"+result);
      });
    }

  }

  // Platform messages are asynchronous, so we initialize in an async method.


  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              RaisedButton(
                child: Text('初始化'),
                onPressed: _init,
              ),
              RaisedButton(
                child: Text('打开'),
                onPressed: _open,
              ),
              RaisedButton(
                child: Text('打印'),
                onPressed:()=>_printText("Hello World!Flutter Plugin"),
              ),


            ],
          )
        ),
      ),
    );
  }

  Future<void> _printText(String text) async {
    print("结果返回"+await _print.print(text));
  }

  Future<void> _init() async {
    print("结果返回"+await _print.init(true));

  }

  Future<void> _open() async {
    print("结果返回"+await _print.open);
  }
}
