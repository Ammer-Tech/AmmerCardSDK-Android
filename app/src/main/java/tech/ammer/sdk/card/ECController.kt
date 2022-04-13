package tech.ammer.sdk.card;

import android.util.Log;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import org.bouncycastle.util.encoders.Hex;

import java.security.KeyFactory;
import java.security.Security;
import java.util.Calendar;


public class ECController {

    private ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
    private SecP256K1Curve curve = new SecP256K1Curve();

    private static ECController controller;

    private ECController() {
        long start = System.currentTimeMillis();
        try {
            byte[] w = new byte[]{4, -11, -41, 13, 21, 100, -38, 88, -62, -14, -61, 88, 29, -73, -107, 124, 104, -36, -118, -22, 108, -72, 42, 43, 124, 81, 96, 40, 124, -44, 79, 102, -92, -14, -32, -55, -27, -1, -27, 35, -114, 52, -109, 5, 91, 77, 95, -40, 36, 55, -40, -8, -31, -100, 115, -85, 106, -44, 8, 99, 30, 109, 100, -90, 51};

            org.bouncycastle.math.ec.ECPoint wPoint = curve.decodePoint(w);

            ECPublicKeySpec cardKeySpec = new ECPublicKeySpec(parameterSpec.getCurve().decodePoint(w), parameterSpec);
            ECPublicKey cardKey = (ECPublicKey) KeyFactory.getInstance("EC", "BC").generatePublic(cardKeySpec);
            String fromCard = Hex.toHexString(cardKey.getEncoded());
            String hardcoded = "3056301006072a8648ce3d020106052b8104000a03420004f5d70d1564da58c2f2c3581db7957c68dc8aea6cb82a2b7c5160287cd44f66a4f2e0c9e5ffe5238e3493055b4d5fd82437d8f8e19c73ab6ad408631e6d64a633";

            Log.d("EOController", fromCard);
            Log.d("EOController", hardcoded);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("EOController", "ECController init: " + (Calendar.getInstance().getTime().getTime() - start) + "ms");
    }

    public static ECController getInstance() {
        if (controller == null) {
            Security.removeProvider("BC");
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
            controller = new ECController();

            return controller;
        }
        return controller;
    }

    public String getPublicKeyString(byte[] w) {
        try {
            ECPublicKeySpec cardKeySpec = new ECPublicKeySpec(parameterSpec.getCurve().decodePoint(w), parameterSpec);
            ECPublicKey cardKey = (ECPublicKey) KeyFactory.getInstance("EC", "BC").generatePublic(cardKeySpec);
            return Hex.toHexString(cardKey.getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
