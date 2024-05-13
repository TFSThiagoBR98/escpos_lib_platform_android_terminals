import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class PaxPrinter extends PlatformInterface {
  /// Constructs a PaxSdkPlatform.
  PaxPrinter() : super(token: _token);

  static final Object _token = Object();

  static PaxPrinter _instance = PaxPrinter();

  /// The default instance of [PaxSdkPlatform] to use.
  ///
  /// Defaults to [MethodChannelPaxSdk].
  static PaxPrinter get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [PaxSdkPlatform] when
  /// they register themselves.
  static set instance(PaxPrinter instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  @visibleForTesting
  final methodChannel = const MethodChannel('br.dev.tfs.escpos_lib/terminals/pax');

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
