package com.ims.bpcluat.helper;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.ims.bpcluat.ApiReqRes;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiHelper {
    NetworkingApiCallBack apiCallBack;
    public static String uFillEndpoint = "ufill";
    public static String alpEndpoint = "alpreq";
    public static String uploadFileUrl = "https://www.uat.fdmerchantservices.com/esb-imswebservicesv2/uploadFuelAppLog";

    /* Start :  UAT */
//    private static String baseUrl = "https://www.uat.fdmerchantservices.com/L3Services/v1/BPCL/";
//    private static String loginBaseUrl = "https://www.uat.fdmerchantservices.com/L3Services/v2/BPCL/";
//    private static String signKey = "PBVC1ZCFE2JQWHMYYTCOBDMPLEFMMHP4";
//    private static String encKey = "BVPW9XDVBG7SRGDZFRK7V1G6BUBBKAH7";
    /* End :  UAT */

    /* Start :  Production */
    private static String baseUrl = "https://www4.firstdatamerchantservices.com/L3Services/v1/BPCL/";
    private static String loginBaseUrl = "https://www4.firstdatamerchantservices.com/L3Services/v2/BPCL/";
    private static String signKey = "U4Y1IZVLQIYRWWNNRTOP3EDZYWE5RCPO";
    private static String encKey = "FDJJBL1SGLHM5HI2UWZRTRCBTY4XQ1A7";
    /* End :  Production */

    public void setApiCallBack(NetworkingApiCallBack apiCallBack){
        this.apiCallBack = apiCallBack;
    }

    public String encryptreq(String jsondata, String SequenceNo) {
        String result = "";
        try {
            Date now = new Date();
            String SUBJECT = "API_SERVICE";
            String AUDIENCE = "OCEAN";
            String ISSUER = "TERMINAL";
            String PAYLOAD = "PAYLOAD";
            Date ISSUETIME = now;
            //long theRandomNum = (long) (Math.random()*Math.pow(14,10));
            String JWT_ID = SequenceNo;
            int ACCESS_TOKEN_VALIDITY_SECONDS = 10 * 60 * 1000;
            byte[] sharedSecret = signKey.getBytes();
            byte[] encSecret = encKey.getBytes();
            String req = jsondata;
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(SUBJECT)
                    .audience(AUDIENCE)
                    .issuer(ISSUER)
                    .issueTime(ISSUETIME)
                    .jwtID(JWT_ID)
                    .expirationTime(new Date(now.getTime() + 1000 * 60 * 10))
                    .claim(PAYLOAD, req).build();

            JWSSigner jws = new MACSigner(sharedSecret);
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(jws);

            JWEObject jweObject = new JWEObject(new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A128CBC_HS256).contentType("JWT").build(), new Payload(signedJWT));
            jweObject.encrypt(new DirectEncrypter(encSecret));
            String encrypt = jweObject.serialize();

            //Log.d("encrypt", "execute: " + encrypt);
            result = encrypt;

        } catch (JOSEException e) {
            //Log.d("Unable to sign JWT: {}.", "execute: "+e.getMessage());
            result = null;
        }
        return result;
    }

    public String decryptreq(String encdata, String SequenceNo) {
        String result = "";
        try {
            Date now = new Date();
            String SUBJECT = "API_SERVICE";
            String AUDIENCE = "OCEAN";
            String ISSUER = "TERMINAL";
            String PAYLOAD = "PAYLOAD";
            Date ISSUETIME = now;
            //long theRandomNum = (long) (Math.random()*Math.pow(14,10));
            String JWT_ID = SequenceNo;
            int ACCESS_TOKEN_VALIDITY_SECONDS = 10 * 60 * 1000;
            byte[] sharedSecret = signKey.getBytes();
            byte[] encSecret = encKey.getBytes();
            String encrypt = encdata;

            String ALG = "DIR";
            String ENC = "A128CBC-HS256";
            JWEObject jwe = JWEObject.parse(encrypt);
            String alg = jwe.getHeader().getAlgorithm().getName();
            String enc = jwe.getHeader().getEncryptionMethod().getName();
            Date currentTime = Calendar.getInstance().getTime();
            String seqNo = "";

            jwe.decrypt(new DirectDecrypter(encSecret));

            SignedJWT signJwt = jwe.getPayload().toSignedJWT();

            if (!signJwt.verify(new MACVerifier(encKey))) {

            }

            JWTClaimsSet claimsSet = signJwt.getJWTClaimsSet();

            final Date expiration = claimsSet.getExpirationTime();
            boolean isExpire = expiration.before(currentTime);
            if (isExpire) {
                //ERROR service time expire.
            }
            //JWT ID check

            JSONObject jsonObject = new JSONObject(String.valueOf(claimsSet));

            Gson gson = new Gson();
            String res1 = gson.toJson(jsonObject);
            result = res1;
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    public void networking(JSONObject args,String apiName, String timeoutstr) {
        String url = apiName;
        String jsondata = String.valueOf(args);
        if (url.equals("userLogin") || url.equals("userCreation") || url.equals("updateUserStatus") || url.equals("resetOprPin") || url.equals("resetTPin") || url.equals("userLogout") || url.equals("lockOrUnlockOperator")) {
            url = loginBaseUrl + url;
        } else {
            url = baseUrl + url;
        }

        Log.d("fiservUrl= ",url);

        Integer timeout = Integer.parseInt(timeoutstr);

        final String SequenceNo = "BPCL" + UUID.randomUUID().toString().substring(0, 7);
        try {
            String encrypt = encryptreq(jsondata, SequenceNo);
            OkHttpClient client = new OkHttpClient();
            // SharedPreferences sharedPreferences = cordova.getActivity().getSharedPreferences("nacResponseSharedPreferences", Context.MODE_PRIVATE);
            // String isNacValue = (sharedPreferences.getString("isNacValue",""));
            /* Start : Proxy Code */
            //  if(!isNacValue.isEmpty()){
            //   if(isNacValue.equals("true")){
            //     client = new OkHttpClient.Builder()
            //             .connectTimeout(timeout, TimeUnit.SECONDS)
            //             .writeTimeout(timeout, TimeUnit.SECONDS)
            //             .readTimeout(timeout, TimeUnit.SECONDS)
            //             .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("cloudapacpos.fiservclients.com", 443)))
            //             .socketFactory(new DelegatingSocketFactory(SSLSocketFactory.getDefault()))
            //             .connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS))
            //             .build();

            //   }else{
            //     client = new OkHttpClient.Builder()
            //             .connectTimeout(timeout, TimeUnit.SECONDS)
            //             .writeTimeout(timeout, TimeUnit.SECONDS)
            //             .readTimeout(timeout, TimeUnit.SECONDS)
            //             .build();
            //   }
            // }
            /* End : Proxy Code */

            /* Start : Without Proxy Code */
            client = new OkHttpClient.Builder()
                    .connectTimeout(timeout, TimeUnit.SECONDS)
                    .writeTimeout(timeout, TimeUnit.SECONDS)
                    .readTimeout(timeout, TimeUnit.SECONDS)
                    .build();
            /* End : Without Proxy Code */

            String postUrl = url;
            String postBody;
            ApiReqRes apiReqRes = new ApiReqRes();
            apiReqRes.setSequenceNo(SequenceNo);
            apiReqRes.setPayload(encrypt);

            Gson gson = new Gson();
            postBody = gson.toJson(apiReqRes);

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, postBody);

            Long requestTime = System.currentTimeMillis();

            Request request = new Request.Builder()
                    .url(postUrl)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Long responseTime = System.currentTimeMillis();
                    Long totalApiTime = (responseTime - requestTime) / 1000;
                    String ts = totalApiTime.toString();

                    String responseData = response.body().string();
                    Log.d("bodyresponsedata", "onResponse: "+responseData);

                    JSONObject jsonObject = null;

                    String payload = "";
                    String statuscode = "";
                    String sequenceNo = "";
                    try {
                        jsonObject = new JSONObject(responseData);
                        payload = jsonObject.getString("payload");
                        statuscode = jsonObject.getString("statusCode");
                        sequenceNo = jsonObject.getString("sequenceNo");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (statuscode.equals("200")) {
                        String decresult = decryptreq(payload, SequenceNo);
                        decresult = decresult.replaceAll("[\\\\]{1}[\"]{1}", "\"");
                        decresult = decresult.substring(decresult.indexOf("{"), decresult.lastIndexOf("}") + 1);
                        decresult = decresult.replaceAll("\"\\{", "{").replaceAll("\\}\"", "}");
                        Log.d("sonuResult",decresult);
                        apiCallBack.apiResult(decresult,apiName);
                    } else {
                        Log.d("sonuTest","1 Line");
                        apiCallBack.apiResult("Server Time Out",apiName);
//                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "404");
//                        callbackContext.sendPluginResult(pluginResult);
                    }
                }
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.d("sonuTest","2 Line"+ e);
                    e.printStackTrace();
                    apiCallBack.apiResult("Server Time Out",apiName);
//                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "403");
//                    callbackContext.sendPluginResult(pluginResult);
                }
            });
            //return true;
        } catch (Exception e) {
            Log.d("sonuTest","3 Line"+ e.toString());
//            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, e.toString());
//            callbackContext.sendPluginResult(pluginResult);
            //return true;
        }
    }

    public interface NetworkingApiCallBack {
        void apiResult(String res, String apiName);
    }
}
