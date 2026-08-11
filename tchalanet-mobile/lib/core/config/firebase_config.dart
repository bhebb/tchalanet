import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';

import '../../firebase_options.dart';

// firebase_core silently rewrites 'localhost'/'127.0.0.1' to 10.0.2.2 on
// Android (emulator assumption), which breaks physical devices reached through
// `adb reverse tcp:9099 tcp:9099`. 'localtest.me' resolves to 127.0.0.1 via
// public DNS without triggering that rewrite, so it works for both the
// emulator and USB-attached devices. Override via --dart-define if needed.
const firebaseAuthEmulatorHost = String.fromEnvironment(
  'FIREBASE_AUTH_EMULATOR_HOST',
  defaultValue: 'localtest.me',
);
const firebaseAuthEmulatorPort = int.fromEnvironment(
  'FIREBASE_AUTH_EMULATOR_PORT',
  defaultValue: 9099,
);
const useFirebaseAuthEmulator = bool.fromEnvironment(
  'FIREBASE_AUTH_EMULATOR',
  defaultValue: false,
);

Future<void> initializeFirebaseAuth() async {
  await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
  if (useFirebaseAuthEmulator) {
    await FirebaseAuth.instance.useAuthEmulator(
      firebaseAuthEmulatorHost,
      firebaseAuthEmulatorPort,
    );
  }
}
