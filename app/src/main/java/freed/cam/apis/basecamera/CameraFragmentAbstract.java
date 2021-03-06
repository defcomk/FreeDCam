/*
 *
 *     Copyright (C) 2015 Ingo Fuchs
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * /
 */

package freed.cam.apis.basecamera;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import freed.ActivityInterface;
import freed.cam.apis.basecamera.modules.ModuleHandlerAbstract;
import freed.cam.apis.basecamera.parameters.AbstractParameterHandler;
import freed.renderscript.RenderScriptManager;
import freed.renderscript.RenderScriptProcessorInterface;
import freed.utils.Log;

/**
 * Created by troop on 06.06.2015.
 * That Fragment is used as base for all camera apis added.
 */
public abstract class CameraFragmentAbstract<P extends AbstractParameterHandler,C extends CameraHolderAbstract> extends Fragment implements CameraInterface ,CameraWrapperInterface {
    private final String TAG = CameraFragmentAbstract.class.getSimpleName();

    protected View view;
    protected RenderScriptManager renderScriptManager;

    public ModuleHandlerAbstract moduleHandler;
    /**
     * parameters for avail for the cameraHolder
     */
    public P parametersHandler;
    /**
     * holds the current camera
     */
    public C cameraHolder;
    /**
     * handels focus releated stuff for the current camera
     */
    public AbstractFocusHandler focusHandler;

    protected boolean PreviewSurfaceRdy;
    private ActivityInterface activityInterface;

    /**
     * holds handler to invoke stuff in ui or camera thread
     */
    protected MainToCameraHandler mainToCameraHandler;
    protected CameraToMainHandler cameraToMainHandler;

    public static CameraFragmentAbstract getInstance()
    {
        return null;
    }

    public CameraFragmentAbstract()
    {

    }

    public void init(MainToCameraHandler mainToCameraHandler, CameraToMainHandler cameraToMainHandler, ActivityInterface activityInterface)
    {
        Log.d(TAG, "init handler");
        this.mainToCameraHandler = mainToCameraHandler;
        this.cameraToMainHandler = cameraToMainHandler;
        this.activityInterface = activityInterface;

    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Log.d(TAG, "onCreateView");

        return super.onCreateView(layoutInflater, viewGroup, null);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(null);
    }

    @Override
    public void onDestroyView()
    {
        Log.d(TAG, "onDestroyView");

        super.onDestroyView();

    }
    
    public void setRenderScriptManager(RenderScriptManager renderScriptManager)
    {
        this.renderScriptManager = renderScriptManager;
    }

    @Override
    public void startCameraAsync() {
        Log.d(TAG, "startCameraAsync");
        if (mainToCameraHandler != null)
            mainToCameraHandler.startCamera();
        else
            Log.d(TAG, "MainToCameraHandler is null");
    }

    @Override
    public void stopCameraAsync() {
        if (mainToCameraHandler != null)
            mainToCameraHandler.stopCamera();
        else
            Log.d(TAG, "MainToCameraHandler is null");
    }

    @Override
    public void restartCameraAsync() {
        if (mainToCameraHandler != null)
            mainToCameraHandler.restartCamera();
        else
            Log.d(TAG, "MainToCameraHandler is null");
    }

    @Override
    public void startPreviewAsync() {
        if (mainToCameraHandler != null)
            mainToCameraHandler.startPreview();
        else
            Log.d(TAG, "MainToCameraHandler is null");
    }

    @Override
    public void stopPreviewAsync() {
        if (mainToCameraHandler != null)
            mainToCameraHandler.stopPreview();
        else
            Log.d(TAG, "MainToCameraHandler is null");
    }

    @Override
    public void restartPreviewAsync() {
        if (mainToCameraHandler != null)
            mainToCameraHandler.restartPreview();
        else
            Log.d(TAG, "MainToCameraHandler is null");
    }

    public abstract int getMargineLeft();
    public abstract int getMargineRight();
    public abstract int getMargineTop();
    public abstract int getPreviewWidth();
    public abstract int getPreviewHeight();

    @Override
    public RenderScriptManager getRenderScriptManager() {
        return renderScriptManager;
    }

    @Override
    public ActivityInterface getActivityInterface() {
        return activityInterface;
    }

    @Override
    public boolean isAeMeteringSupported() {
        return focusHandler.isAeMeteringSupported();
    }

    @Override
    public RenderScriptProcessorInterface getFocusPeakProcessor() {
        return null;
    }

    @Override
    public AbstractFocusHandler getFocusHandler() {
        return focusHandler;
    }

    @Override
    public C getCameraHolder() {
        return cameraHolder;
    }

    @Override
    public P getParameterHandler() {
        return parametersHandler;
    }

    @Override
    public ModuleHandlerAbstract getModuleHandler() {
        return moduleHandler;
    }

}
