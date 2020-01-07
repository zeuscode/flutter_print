import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:print_pda/print_pda.dart';

void main() {
  const MethodChannel channel = MethodChannel('print_pda');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });


}
