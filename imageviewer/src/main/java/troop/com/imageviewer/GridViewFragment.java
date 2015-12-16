package troop.com.imageviewer;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.defcomk.jni.libraw.RawUtils;
import com.troop.freedcam.utils.StringUtils;
import com.troop.marshmallowpermission.MPermissions;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by troop on 11.12.2015.
 */
public class GridViewFragment extends Fragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener
{
    GridView gridView;
    View view;
    private ImageAdapter mPagerAdapter;
    FileHolder[] files;
    int mImageThumbSize = 0;
    CacheHelper cacheHelper;
    final String TAG = GridViewFragment.class.getSimpleName();
    private ViewStates currentViewState = ViewStates.normal;
    private Button deleteButton;
    private Button gobackButton;
    private Button filetypeButton;
    final String savedInstanceString = "lastpath";
    String savedInstanceFilePath;
    FormatTypes formatsToShow = FormatTypes.all;


    public enum ViewStates
    {
        normal,
        selection,
    }

    public enum FormatTypes
    {
        all,
        raw,
        dng,
        jpg,
        jps,
        mp4,
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.gridviewfragment, container, false);
        this.gridView = (GridView) view.findViewById(R.id.gridView);
        gridView.setOnItemClickListener(this);
        gridView.setOnItemLongClickListener(this);
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        deleteButton = (Button)view.findViewById(R.id.button_deltePics);
        deleteButton.setVisibility(View.GONE);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentViewState) {
                    case normal:
                        setViewMode(ViewStates.selection);
                        break;
                    case selection:
                        //check if files are selceted
                        boolean hasfilesSelected = false;
                        for (FileHolder f : files) {
                            if (f.IsSelected()) {
                                hasfilesSelected = true;
                                break;
                            }

                        }
                        //if no files selected skip dialog
                        if (!hasfilesSelected)
                            break;
                        //else show dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage("Delete File's?").setPositiveButton("Yes", dialogClickListener)
                                .setNegativeButton("No", dialogClickListener).show();
                        setViewMode(ViewStates.normal);
                        break;
                }
            }
        });

        gobackButton = (Button)view.findViewById(R.id.button_goback);
        gobackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (currentViewState == ViewStates.normal) {
                    if (files != null && files.length > 0) {
                        String topPath = files[0].getFile().getParentFile().getParentFile().getAbsolutePath() + "/";
                        String inter = StringUtils.GetInternalSDCARD() + StringUtils.DCIMFolder;
                        String external = StringUtils.GetExternalSDCARD() + StringUtils.DCIMFolder;

                        if ((inter.contains(topPath) && topPath.length() < inter.length() || topPath.equals(inter))
                                || (external.contains(topPath) && topPath.length() < external.length() || topPath.equals(external)))
                        {
                            savedInstanceFilePath =null;
                            loadDefaultFolders();
                        }
                        else {
                            loadFiles(files[0].getFile());
                            savedInstanceFilePath = files[0].getFile().getAbsolutePath();
                        }
                    }
                    else
                        loadDefaultFolders();
                }
                else if (currentViewState == ViewStates.selection)
                {
                    for (int i = 0; i< files.length; i++)
                    {
                        FileHolder f = files[i];
                        f.SetSelected(false);
                    }
                    setViewMode(ViewStates.normal);
                }
            }
        });

        filetypeButton = (Button)view.findViewById(R.id.button_filetype);
        filetypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup(v);
            }
        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            savedInstanceFilePath = (String) savedInstanceState.get(savedInstanceString);

        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(savedInstanceString, savedInstanceFilePath);
        super.onSaveInstanceState(outState);
    }

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:

                    for (FileHolder f: files)
                    {
                        if (f.IsSelected())
                        {
                            boolean d = f.getFile().delete();
                            Log.d(TAG,"File delted:" + f.getFile().getName() + " :" + d);
                        }
                    }
                    loadFiles(new File(savedInstanceFilePath));
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };



    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cacheHelper = new CacheHelper(getActivity());
        if (Build.VERSION.SDK_INT > 22 &&getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            MPermissions.requestSDPermission(this);
        }
        if(savedInstanceState != null){
            savedInstanceFilePath = (String) savedInstanceState.get(savedInstanceString);

        }



    }

    @Override
    public void onResume() {
        super.onResume();
        if (savedInstanceFilePath == null)
            loadDefaultFolders();
        else
            loadFiles(new File(savedInstanceFilePath));
    }


    private void loadDefaultFolders()
    {
        File internalSDCIM = new File(StringUtils.GetInternalSDCARD() + StringUtils.DCIMFolder);
        File externalSDCIM = new File(StringUtils.GetExternalSDCARD() + StringUtils.DCIMFolder);
        ArrayList<FileHolder> list = new ArrayList<FileHolder>();
        File[] f = internalSDCIM.listFiles();
        for (int i = 0; i< f.length; i++)
        {
            if (!f[i].isHidden())
                list.add(new FileHolder(f[i]));
        }
        try {
            f = externalSDCIM.listFiles();
            for (int i = 0; i< f.length; i++)
            {
                if (!f[i].isHidden())
                    list.add(new FileHolder(f[i]));
            }
        }
        catch (Exception ex) {
            Log.d(TAG, "No external SD!");
        }
        files = new FileHolder[list.size()];
        list.toArray(files);
        sortList(files);
        mPagerAdapter = new ImageAdapter(getContext());
        gridView.setAdapter(mPagerAdapter);
    }

    private void loadFiles(File file)
    {
        ArrayList<FileHolder> list = new ArrayList<FileHolder>();
        File[]f = file.listFiles();
        for (int i = 0; i< f.length; i++)
        {
            if (!f[i].isHidden()) {
                if (formatsToShow == FormatTypes.all && (
                        f[i].getAbsolutePath().endsWith("jpg")
                        || f[i].getAbsolutePath().endsWith("jps")
                        ||f[i].getAbsolutePath().endsWith("raw")
                        ||f[i].getAbsolutePath().endsWith("dng")
                                ||   f[i].getAbsolutePath().endsWith("mp4")
                ))
                    list.add(new FileHolder(f[i]));
                else if(formatsToShow == FormatTypes.dng && f[i].getAbsolutePath().endsWith("dng"))
                    list.add(new FileHolder(f[i]));
                else if(formatsToShow == FormatTypes.raw && f[i].getAbsolutePath().endsWith("raw"))
                    list.add(new FileHolder(f[i]));
                else if(formatsToShow == FormatTypes.jps && f[i].getAbsolutePath().endsWith("jps"))
                    list.add(new FileHolder(f[i]));
                else if(formatsToShow == FormatTypes.jpg && f[i].getAbsolutePath().endsWith("jpg"))
                    list.add(new FileHolder(f[i]));
                else if(formatsToShow == FormatTypes.mp4 && f[i].getAbsolutePath().endsWith("mp4"))
                    list.add(new FileHolder(f[i]));
            }
        }
        files = new FileHolder[list.size()];
        list.toArray(files);
        sortList(files);
        mPagerAdapter = new ImageAdapter(getContext());
        gridView.setAdapter(mPagerAdapter);
    }

    private void sortList(FileHolder[] filesar)
    {
        Arrays.sort(filesar, new Comparator<FileHolder>() {
            public int compare(FileHolder f1, FileHolder f2) {
                return Long.valueOf(f1.getFile().lastModified()).compareTo(f2.getFile().lastModified());
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        switch (currentViewState)
        {
            case normal:
                if (!files[position].IsFolder()) {
                    final Intent i = new Intent(getActivity(), ScreenSlideActivity.class);
                    i.putExtra(ScreenSlideActivity.EXTRA_IMAGE, position);
                    if (files != null &&files.length >0)
                    {
                        if (!files[position].IsFolder())
                            i.putExtra(ScreenSlideActivity.IMAGE_PATH, files[position].getFile().getParentFile().getAbsolutePath());
                        else
                            i.putExtra(ScreenSlideActivity.IMAGE_PATH, files[position].getFile().getAbsolutePath());
                    }
                    i.putExtra(ScreenSlideActivity.FileType, formatsToShow.name());
                    startActivity(i);
                }
                else
                {
                    savedInstanceFilePath = files[position].getFile().getAbsolutePath();
                    loadFiles(files[position].getFile());

                }
                break;
            case selection:
                if (files[position].IsSelected())
                    files[position].SetSelected(false);
                else
                    files[position].SetSelected(true);
        }
    }


    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
    {
        switch (currentViewState)
        {
            case normal:
                setViewMode(ViewStates.selection);
                files[position].SetSelected(true);
        }
        return false;
    }



    private class ImageAdapter extends BaseAdapter {
        private final Context mContext;

        public ImageAdapter(Context context) {
            super();
            mContext = context;
        }

        @Override
        public int getCount() {
            return files.length;
        }

        @Override
        public Object getItem(int position) {
            return files[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            GridImageView imageView;
            if (convertView == null) { // if it's not recycled, initialize some attributes
                imageView = new GridImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(
                        AbsoluteLayout.LayoutParams.MATCH_PARENT, AbsoluteLayout.LayoutParams.MATCH_PARENT));
            } else {
                imageView = (GridImageView) convertView;
            }
            //Set FileHolder to current imageview
            imageView.SetEventListner(files[position]);
            Log.d(TAG, "pos:" + position +  "imageviewState: " + files[position].GetCurrentViewState()+ "/GridState:" + currentViewState + "filename:" + files[position].getFile().getName()+
                    "ischecked:" + files[position].IsSelected());
            loadBitmap(files[position].getFile(), imageView); // Load image into ImageView
            return imageView;
        }
    }

    private static BitmapWorkerTask getBitmapWorkerTask(GridImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public void loadBitmap(File file, GridImageView imageView)
    {
        if (cancelPotentialWork(file, imageView))
        {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            if (!file.isDirectory())
            {
                final AsyncDrawable asyncDrawable =
                        new AsyncDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.noimage), task);
                imageView.setImageDrawable(asyncDrawable);
            }
            else
            {
                final AsyncDrawable asyncDrawable =
                        new AsyncDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.folder), task);
                imageView.setImageDrawable(asyncDrawable);
            }
            String f = file.getName();
            if (!file.isDirectory())
                imageView.SetFileEnding(f.substring(f.length()-3));
            else
                imageView.SetFileEnding(f);
            task.execute(file);
        }
    }


    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    public static boolean cancelPotentialWork(File file, GridImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final File bitmapData = bitmapWorkerTask.file;
            if (bitmapData != file) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    class BitmapWorkerTask extends AsyncTask<File, Void, Bitmap> {
        private final WeakReference<GridImageView> imageViewReference;
        private File file;

        public BitmapWorkerTask(GridImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<GridImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(File... params) {
            file = params[0];
            return getBitmap(file);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final GridImageView imageView = imageViewReference.get();
                if (imageView != null)
                {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private Bitmap getBitmap(File file)
    {
        if (!file.getAbsolutePath().endsWith("jpg")
                && !file.getAbsolutePath().endsWith("dng")
                && !file.getAbsolutePath().endsWith("mp4")
                && !file.getAbsolutePath().endsWith("raw")
                && !file.getAbsolutePath().endsWith("jps"))
            return null;

        Bitmap response = cacheHelper.getBitmapFromMemCache(file.getName());
        if (response == null)
        {
            //Log.d(TAG,"No image in memory try from disk");
            response = cacheHelper.getBitmapFromDiskCache(file.getName());
        }
        if (response == null)
        {
            //Log.d(TAG,"No image in thumbcache try from disk");
            if (file.getAbsolutePath().endsWith(".jpg")) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 20;
                response = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                response = ThumbnailUtils.extractThumbnail(response, mImageThumbSize, mImageThumbSize);
                if (response != null) {
                    cacheHelper.addBitmapToMemoryCache(file.getName(), response);
                    cacheHelper.addBitmapToCache(file.getName(), response);
                }

            } else if (file.getAbsolutePath().endsWith(".mp4")) {
                response = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
                response = ThumbnailUtils.extractThumbnail(response, mImageThumbSize, mImageThumbSize);
                if (response != null)
                {
                    cacheHelper.addBitmapToCache(file.getName(), response);
                    cacheHelper.addBitmapToMemoryCache(file.getName(), response);
                }
            } else if (file.getAbsolutePath().endsWith(".dng") || file.getAbsolutePath().endsWith(".raw")) {
                try
                {
                    response = RawUtils.UnPackRAW(file.getAbsolutePath());
                    if (response != null)
                        response.setHasAlpha(true);
                    response = ThumbnailUtils.extractThumbnail(response, mImageThumbSize, mImageThumbSize);
                    if (response != null) {
                        cacheHelper.addBitmapToMemoryCache(file.getName(), response);
                        cacheHelper.addBitmapToCache(file.getName(), response);
                    }
                } catch (IllegalArgumentException ex) {
                    response = null;

                }
            }
        }


        return response;
    }

    private void setViewMode(ViewStates viewState)
    {
        this.currentViewState = viewState;
        for (int i = 0; i< files.length; i++)
        {
            FileHolder f = files[i];
            f.SetViewState(viewState);

        }
        switch (viewState)
        {
            case normal:
            {
                deleteButton.setVisibility(View.GONE);
                break;
            }
            case selection:
                deleteButton.setVisibility(View.VISIBLE);
                break;

        }
    }

    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this.getContext(), v);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int i = item.getItemId();
                if (i == R.id.all)
                {
                    filetypeButton.setText("All");
                    formatsToShow = FormatTypes.all;
                }
                else if (i == R.id.raw)
                {
                    filetypeButton.setText("RAW");
                    formatsToShow = FormatTypes.raw;
                }
                else if (i == R.id.dng)
                {
                    filetypeButton.setText("DNG");
                    formatsToShow = FormatTypes.dng;
                }
                else if (i == R.id.jps)
                {
                    filetypeButton.setText("jPS");
                    formatsToShow = FormatTypes.jps;
                }
                else if (i == R.id.jpg)
                {
                    filetypeButton.setText("JPG");
                    formatsToShow = FormatTypes.jpg;
                }
                else if (i == R.id.mp4)
                {
                    filetypeButton.setText("MP$");
                    formatsToShow = FormatTypes.mp4;
                }
                if (savedInstanceFilePath != null)
                    loadFiles(new File(savedInstanceFilePath));
                return false;

            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.filetypepopupmenu, popup.getMenu());
        popup.show();
    }

}
