package com.mvp.lt.arrr.arlib.control;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import com.mvp.lt.arrr.arlib.PARFragment;
import com.mvp.lt.arrr.arlib.PARInstallation;
import com.mvp.lt.arrr.arlib.enumm.PSKDeviceOrientation;
import com.mvp.lt.arrr.arlib.listener.PSKEventListener;
import com.mvp.lt.arrr.arlib.listener.PSKSensorManager;
import com.mvp.lt.arrr.arlib.poi.PARPoi;
import com.mvp.lt.arrr.arlib.poi.PARPoiLabel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PARController implements PSKEventListener {
    public static boolean DEBUG = true;
    public static float CLIP_POIS_NEARER_THAN = 5.0F;
    public static float CLIP_POIS_FARER_THAN = 1.0E7F;
    public static PARDataCollector dataCollector;
    private static PARController _sharedPARController = new PARController();
    private static Context _context;
    private static String _apiKey = "";
    private static boolean _hasValidApiKey = false;
    private static ArrayList<PARPoi> _pois;
    private String TAG = "PARController";

    public static String getFrameworkVersion() {
        return "1.0.1562";
    }

    private PARController() {
        _pois = new ArrayList();
    }

    public static Context getContext() {
        return _context;
    }

    public static PARController getInstance() {
        return _sharedPARController;
    }

    public static ArrayList<PARPoi> getPois() {
        return _pois;
    }

    public void init(Context ctx) {
        _context = ctx;
        Log.i(this.TAG, "init()");

        PARDataCollector dataCollector = new PARDataCollector();
        String osVersion = Build.VERSION.RELEASE;
        String deviceId = PARInstallation.id(ctx);

        PSKSensorManager.getSharedSensorManager().setEventListener(this);
    }


    public boolean hasValidApiKey() {
        return _hasValidApiKey;
    }

    public void addPoi(PARPoi poi) {
        if (_pois.contains(poi)) {
            Log.e(this.TAG, "PARPoi not added (same PARPoi already added to PARController).");
        } else {
            _pois.add(poi);
            poi.updateLocation();
            poi.onAddedToARController();
        }
    }

    public void addPois(ArrayList<PARPoi> anArray) {
        Iterator i$ = anArray.iterator();

        while (i$.hasNext()) {
            PARPoi arPoi = (PARPoi) i$.next();
            this.addPoi(arPoi);
        }

    }

    public void removeObject(PARPoi poi) {
        if (!_pois.contains(poi)) {
            Log.e(this.TAG, "PARPoi not removed (not added to PARController).");
        } else {
            poi.onRemovedFromARController();
            _pois.remove(poi);
        }
    }

    public void removeObject(int index) {
        try {
            if (index >= 0 && index < _pois.size()) {
                this.removeObject((PARPoi) _pois.get(index));
            }
        } catch (NullPointerException var3) {
            var3.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException var4) {
            var4.printStackTrace();
        }

    }

    public void clearObjects() {
        try {
            ListIterator iterator = _pois.listIterator();

            while (iterator.hasNext()) {
                PARPoi poi = (PARPoi) iterator.next();
                poi.onRemovedFromARController();
                Log.wtf(this.TAG, "Removing: " + ((PARPoiLabel) poi).getTitle());
                iterator.remove();
            }
        } catch (NoSuchElementException var4) {
            var4.printStackTrace();
        } catch (UnsupportedOperationException var5) {
            var5.printStackTrace();
        } catch (Exception var6) {
            try {
                while (_pois.size() > 0) {
                    this.removeObject(0);
                }
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        }

    }

    public int numberOfObjects() {
        Log.wtf(this.TAG, "_poi.size = " + _pois.size());
        return _pois.size();
    }

    public PARPoi getObject(int index) {
        return index >= 0 && index < _pois.size() ? (PARPoi) _pois.get(index) : null;
    }

    @Override
    public void onLocationChangedEvent(Location location) {
        Iterator i$ = _pois.iterator();

        while (i$.hasNext()) {
            PARPoi poi = (PARPoi) i$.next();
            poi.updateLocation();
        }

        this.sortMarkersByDistance();
        if (PARFragment.getActiveFragment() != null) {
            PARFragment.getActiveFragment().onLocationChangedEvent(location);
        }

    }

    @Override
    public void onDeviceOrientationChanged(PSKDeviceOrientation newOrientation) {
        if (PARFragment.getActiveFragment() != null) {
            PARFragment.getActiveFragment().onDeviceOrientationChanged(newOrientation);
        }

    }

    private void sortMarkersByDistance() {
        try {
            Collections.sort(_pois, new Comparator<PARPoi>() {
                @Override
                public int compare(PARPoi parPoi1, PARPoi parPoi2) {
                    return parPoi1.distanceToUser >= parPoi2.distanceToUser ? 1 : (parPoi1.distanceToUser < parPoi2.distanceToUser ? -1 : 0);
                }
            });
        } catch (NullPointerException var2) {
            Log.wtf(this.TAG, "Sort objects failed.");
            var2.printStackTrace();
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }


}

