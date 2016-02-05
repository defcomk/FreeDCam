package com.troop.freedcam.camera.parameters.modes;

import android.os.Handler;

import com.troop.freedcam.camera.BaseCameraHolder;
import com.troop.freedcam.camera.modules.ModuleHandler;

import java.util.HashMap;

/**
 * Created by troop on 05.02.2016.
 */
public class PictureFormatHandler extends BaseModeParameter
{
    private final String PICFORMATVALUES = "picture-format-values";
    private final String PICFORMAT = "picture-format";
    private boolean rawSupported = false;
    private String captureMode = "jpeg";
    private String rawFormat;


    final public String[] CaptureMode =
    {
        "jpeg",
        "raw",
        "dng"
    };

    /***
     * @param uihandler    Holds the ui Thread to invoke the ui from antother thread
     * @param parameters   Hold the Camera Parameters
     * @param cameraHolder Hold the camera object
     */
    public PictureFormatHandler(Handler uihandler, HashMap<String, String> parameters, BaseCameraHolder cameraHolder)
    {
        super(uihandler, parameters, cameraHolder, "", "");
        switch (cameraHolder.DeviceFrameWork)
        {

            case Normal://normal has no break so it runs always through lg
            case LG:
                if (parameters.containsKey(PICFORMATVALUES))
                {
                    isSupported = true;
                    String formats = parameters.get("picture-format-values");
                    if (formats.contains("bayer-mipi") || formats.contains("raw"))
                    {
                        rawSupported = true;
                        String forms[] = formats.split(",");
                        for(String s : forms)
                        {
                            if (s.contains("bayer-mipi") || s.contains("raw")) {
                                rawFormat = s;
                                break;
                            }
                        }
                    }
                }
                break;
            case MTK:
                isSupported = true;
                rawSupported = true;
                break;
        }
    }

    @Override
    public void SetValue(String valueToSet, boolean setToCam)
    {
        captureMode = valueToSet;
        switch (baseCameraHolder.DeviceFrameWork)
        {
            case Normal:
            case LG:
                switch (valueToSet)
                {
                    case "jpeg":
                        setString(valueToSet);
                        break;
                    case "raw":
                        setString(rawFormat);
                        baseCameraHolder.ParameterHandler.SetDngActive(false);
                        break;
                    case "dng":
                        setString(rawFormat);
                        baseCameraHolder.ParameterHandler.SetDngActive(true);
                        break;
                }
                break;
            case MTK:
                //handeld due appsettings
                break;
        }
        if(baseCameraHolder.DeviceFrameWork == BaseCameraHolder.Frameworks.LG && !firststart)
        {
            baseCameraHolder.StopPreview();
            baseCameraHolder.StartPreview();

        }
        firststart = false;
        BackgroundValueHasChanged(valueToSet);
    }

    private void setString(String val)
    {
        parameters.put(PICFORMAT, val);
        baseCameraHolder.SetCameraParameters(parameters);
    }

    @Override
    public boolean IsSupported() {
        return isSupported;
    }

    @Override
    public String GetValue() {
        return captureMode;
    }

    @Override
    public String[] GetValues()
    {
        if (rawSupported)
            return CaptureMode;
        else
            return new String[]{"jpeg"};
    }

    @Override
    public String ModuleChanged(String module)
    {
        switch (module)
        {
            case ModuleHandler.MODULE_PICTURE:
            case ModuleHandler.MODULE_INTERVAL:
            case ModuleHandler.MODULE_HDR:
                BackgroundIsSupportedChanged(true);
                break;
            case ModuleHandler.MODULE_VIDEO:
                BackgroundIsSupportedChanged(false);
                break;
        }
        return super.ModuleChanged(module);
    }

    @Override
    public void onValueChanged(String val) {
        super.onValueChanged(val);
    }

    @Override
    public void onIsSupportedChanged(boolean isSupported) {
        super.onIsSupportedChanged(isSupported);
    }

    @Override
    public void onIsSetSupportedChanged(boolean isSupported) {
        super.onIsSetSupportedChanged(isSupported);
    }

    @Override
    public void onValuesChanged(String[] values) {
        super.onValuesChanged(values);
    }
}