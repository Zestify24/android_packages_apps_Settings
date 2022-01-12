/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.wifi.tether;

import static com.android.settings.AllInOneTetherSettings.DEDUP_POSTFIX;

import android.annotation.NonNull;
import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.util.FeatureFlagUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for logic pertaining to the security type of Wi-Fi tethering.
 */
public class WifiTetherSecurityPreferenceController extends WifiTetherBasePreferenceController
        implements WifiManager.SoftApCallback {

    private static final String PREF_KEY = "wifi_tether_security";

    private final String[] mSecurityEntries;
    private int mSecurityValue;

    public WifiTetherSecurityPreferenceController(Context context,
            OnTetherConfigUpdateListener listener) {
        super(context, listener);
        mSecurityEntries = mContext.getResources().getStringArray(R.array.wifi_tether_security);
    }

    @Override
    public String getPreferenceKey() {
        return FeatureFlagUtils.isEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE)
                ? PREF_KEY + DEDUP_POSTFIX : PREF_KEY;
    }

    @Override
    public void updateDisplay() {
        final SoftApConfiguration config = mWifiManager.getSoftApConfiguration(); 
        if (config != null && config.getSecurityType() == SoftApConfiguration.SECURITY_TYPE_OPEN) {
            mSecurityValue = SoftApConfiguration.SECURITY_TYPE_OPEN;
        } else {
            mSecurityValue = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
        }

        final ListPreference preference = (ListPreference) mPreference;
        preference.setSummary(getSummaryForSecurityType(mSecurityValue));
        preference.setValue(String.valueOf(mSecurityValue));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mSecurityValue = Integer.parseInt((String) newValue);
        preference.setSummary(mSecurityMap.get(mSecurityValue));
        if (mListener != null) {
            mListener.onTetherConfigUpdated(this);
        }
        return true;
    }

    @Override
    public void onCapabilityChanged(@NonNull SoftApCapability softApCapability) {
        final boolean isWpa3Supported =
                softApCapability.areFeaturesSupported(SoftApCapability.SOFTAP_FEATURE_WPA3_SAE);
        if (!isWpa3Supported) {
            Log.i(PREF_KEY, "WPA3 SAE is not supported on this device");
        }

        final boolean isOweSupported =
                softApCapability.areFeaturesSupported(SoftApCapability.SOFTAP_FEATURE_WPA3_OWE);
        if (!isOweSupported) {
            Log.i(PREF_KEY, "OWE not supported.");
        }

        if (mIsWpa3Supported != isWpa3Supported
                || mIsOweSapSupported != isOweSupported) {
            mIsWpa3Supported = isWpa3Supported;
            mIsOweSapSupported = isOweSupported;
            updateDisplay();
        }
        mWifiManager.unregisterSoftApCallback(this);
    }

    public int getSecurityType() {
        return mSecurityValue;
    }

    private String getSummaryForSecurityType(int securityType) {
        if (securityType == SoftApConfiguration.SECURITY_TYPE_OPEN) {
            return mSecurityEntries[1];
        }
        // WPA2 PSK
        return mSecurityEntries[0];
    }
}
