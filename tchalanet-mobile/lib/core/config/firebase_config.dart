import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';

import '../../firebase_options.dart';

const firebaseAuthEmulatorHost = String.fromEnvironment(
  'FIREBASE_AUTH_EMULATOR_HOST',
  defaultValue: '10.0.2.2',
);
const firebaseAuthEmulatorPort = int.fromEnvironment(
  'FIREBASE_AUTH_EMULATOR_PORT',
  defaultValue: 9099,
);
const useFirebaseAuthEmulator = bool.fromEnvironment(
  'FIREBASE_AUTH_EMULATOR',
  defaultValue: true,
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
