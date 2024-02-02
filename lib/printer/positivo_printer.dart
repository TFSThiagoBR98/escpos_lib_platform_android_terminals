import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class PositivoPrinter extends PlatformInterface {
  /// Constructs a PositivoSdkPlatform.
  PositivoPrinter() : super(token: _token);

  static final Object _token = Object();

  static PositivoPrinter _instance = PositivoPrinter();

  /// The default instance of [PositivoSdkPlatform] to use.
  ///
  /// Defaults to [MethodChannelPositivoSdk].
  static PositivoPrinter get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [PositivoSdkPlatform] when
  /// they register themselves.
  static set instance(PositivoPrinter instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  @visibleForTesting
  final methodChannel = const MethodChannel('br.dev.tfs.escpos_lib/terminals/positivo');

  Future<void> startPrinterService() async {
    await methodChannel.invokeMethod<void>('startPrinterService');
  }

  Future<void> printBitmap({required Uint8List image}) async {
    await methodChannel.invokeMethod<void>('printBitmap', <String, dynamic>{'image': image});
  }

  Future<void> printWrapPaper({required int lines}) async {
    await methodChannel.invokeMethod<void>('printWrapPaper', <String, dynamic>{'lines': lines});
  }

  Future<void> printHtml({required String htmlContent}) async {
    await methodChannel.invokeMethod<void>('printHtml', <String, dynamic>{'htmlContent': htmlContent});
  }

  Future<void> initPrinter() async {
    await methodChannel.invokeMethod<void>('initPrinter');
  }

  Future<bool> isAvaliable() async {
    return await methodChannel.invokeMethod<bool>('isAvaliable') ?? false;
  }
}
