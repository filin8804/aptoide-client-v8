package cm.aptoide.pt.spotandshareandroid;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import cm.aptoide.pt.spotandshare.socket.entities.AndroidAppInfo;
import cm.aptoide.pt.spotandshare.socket.entities.FileInfo;
import cm.aptoide.pt.spotandshare.socket.entities.Host;
import cm.aptoide.pt.spotandshare.socket.interfaces.FileClientLifecycle;
import cm.aptoide.pt.spotandshare.socket.interfaces.FileServerLifecycle;
import cm.aptoide.pt.spotandshare.socket.interfaces.HostsChangedCallback;
import cm.aptoide.pt.spotandshare.socket.message.client.AptoideMessageClientSocket;
import cm.aptoide.pt.spotandshare.socket.message.interfaces.StorageCapacity;
import cm.aptoide.pt.spotandshare.socket.message.messages.v1.RequestPermissionToSend;
import cm.aptoide.pt.spotandshare.socket.message.server.AptoideMessageServerSocket;
import cm.aptoide.pt.utils.AptoideUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class HighwayServerService extends Service {

  public static final int INSTALL_APP_NOTIFICATION_REQUEST_CODE = 147;
  private final int PROGRESS_SPLIT_SIZE = 10;
  private NotificationManagerCompat mNotifyManager;
  private Object mBuilderSend;
  private Object mBuilderReceive;
  private FileClientLifecycle<AndroidAppInfo> fileClientLifecycle;
  private FileServerLifecycle<AndroidAppInfo> fileServerLifecycle;

  private List<App> listOfApps;

  private AptoideMessageServerSocket aptoideMessageServerSocket;
  private AptoideMessageClientSocket aptoideMessageClientSocket;

  @Override public void onCreate() {
    super.onCreate();
    if (mNotifyManager == null) {
      mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
    }
    fileClientLifecycle = new FileClientLifecycle<AndroidAppInfo>() {

      private ProgressFilter progressFilter;

      @Override public void onError(IOException e) {
        // Não ta facil perceber pk é k isto cai aqui quando só há um cliente, martelo ftw :/
        if (aptoideMessageServerSocket.getAptoideMessageControllers().size() <= 1) {
          return;
        }

        e.printStackTrace();

        Intent i = new Intent();
        i.setAction("ERRORRECEIVING");
        sendBroadcast(i);
      }

      @Override public void onStartReceiving(AndroidAppInfo androidAppInfo) {

        progressFilter = new ProgressFilter(PROGRESS_SPLIT_SIZE);

        createReceiveNotification(androidAppInfo.getAppName());

        Intent i = new Intent();
        i.putExtra("FinishedReceiving", false);
        i.putExtra("appName", androidAppInfo.getAppName());
        i.setAction("RECEIVEAPP");
        sendBroadcast(i);
      }

      @Override public void onFinishReceiving(AndroidAppInfo androidAppInfo) {

        finishReceiveNotification(androidAppInfo.getApk().getFilePath(),
            androidAppInfo.getPackageName(), androidAppInfo);

        Intent i = new Intent();
        i.putExtra("FinishedReceiving", true);
        i.putExtra("needReSend", false);
        i.putExtra("appName", androidAppInfo.getAppName());
        i.putExtra("packageName", androidAppInfo.getPackageName());
        i.putExtra("filePath", androidAppInfo.getApk().getFilePath());
        i.setAction("RECEIVEAPP");
        sendBroadcast(i);
      }

      @Override public void onProgressChanged(AndroidAppInfo androidAppInfo, float progress) {

        if (progressFilter.shouldUpdate(progress)) {
          int actualProgress = Math.round(progress * 100);
          showReceiveProgress(androidAppInfo.getAppName(), actualProgress, androidAppInfo);
        }
      }
    };

    fileServerLifecycle = new FileServerLifecycle<AndroidAppInfo>() {

      private ProgressFilter progressFilter;

      @Override public void onStartSending(AndroidAppInfo androidAppInfo) {

        progressFilter = new ProgressFilter(PROGRESS_SPLIT_SIZE);

        createSendNotification();

        Intent i = new Intent();
        i.putExtra("isSent", false);
        i.putExtra("needReSend", false);
        i.putExtra("appName", androidAppInfo.getAppName());
        i.putExtra("packageName", androidAppInfo.getPackageName());
        i.putExtra("positionToReSend", 100000);
        i.setAction("SENDAPP");
        sendBroadcast(i);
      }

      @Override public void onFinishSending(AndroidAppInfo androidAppInfo) {
        System.out.println("Server : finished sending " + androidAppInfo);

        finishSendNotification(androidAppInfo);

        Intent i = new Intent();
        i.putExtra("isSent", true);
        i.putExtra("needReSend", false);
        i.putExtra("appName", androidAppInfo.getAppName());
        i.putExtra("packageName", androidAppInfo.getPackageName());
        i.putExtra("positionToReSend", 100000);
        i.setAction("SENDAPP");
        sendBroadcast(i);
      }

      @Override public void onError(IOException e) {
        System.out.println("Fell on error Server !! ");
        e.printStackTrace();
        Intent i = new Intent();
        i.setAction("ERRORSENDING");
        sendBroadcast(i);
      }

      @Override public void onProgressChanged(AndroidAppInfo androidAppInfo, float progress) {
        if (progressFilter.shouldUpdate(progress)) {
          int actualProgress = Math.round(progress * 100);
          showSendProgress(androidAppInfo.getAppName(), actualProgress, androidAppInfo);
        }
      }
    };
  }

  private void createReceiveNotification(String receivingAppName) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      mBuilderReceive = new NotificationCompat.Builder(this);
      ((NotificationCompat.Builder) mBuilderReceive).setContentTitle(
          this.getResources().getString(R.string.spot_share) + " - " + this.getResources()
              .getString(R.string.receive))
          .setContentText(
              this.getResources().getString(R.string.receiving) + " " + receivingAppName)
          .setSmallIcon(R.mipmap.ic_launcher);
    }
  }

  private void finishReceiveNotification(String receivedApkFilePath, String packageName,
      AndroidAppInfo androidAppInfo) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      ((NotificationCompat.Builder) mBuilderReceive).setContentText(
          this.getResources().getString(R.string.transfCompleted))
          .setSmallIcon(android.R.drawable.stat_sys_download_done)
          .setProgress(0, 0, false)
          .setAutoCancel(true);

      Intent intent = new Intent();
      intent.setAction("INSTALL_APP_NOTIFICATION");
      intent.putExtra("filePath", receivedApkFilePath);
      intent.putExtra("packageName", packageName);
      PendingIntent contentIntent =
          PendingIntent.getBroadcast(this, INSTALL_APP_NOTIFICATION_REQUEST_CODE, intent,
              PendingIntent.FLAG_CANCEL_CURRENT);

      ((NotificationCompat.Builder) mBuilderReceive).setContentIntent(contentIntent);
      if (mNotifyManager == null) {
        mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
      }
      mNotifyManager.notify(androidAppInfo.getPackageName().hashCode(),
          ((NotificationCompat.Builder) mBuilderReceive).build());
    }
  }

  private void showReceiveProgress(String receivingAppName, int actual,
      AndroidAppInfo androidAppInfo) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      ((NotificationCompat.Builder) mBuilderReceive).setContentText(
          this.getResources().getString(R.string.receiving) + " " + receivingAppName);

      ((NotificationCompat.Builder) mBuilderReceive).setProgress(100, actual, false);
      if (mNotifyManager == null) {
        mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
      }
      mNotifyManager.notify(androidAppInfo.getPackageName().hashCode(),
          ((NotificationCompat.Builder) mBuilderReceive).build());
    }
  }

  private void createSendNotification() {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      mBuilderSend = new NotificationCompat.Builder(this);
      ((NotificationCompat.Builder) mBuilderSend).setContentTitle(
          this.getResources().getString(R.string.spot_share) + " - " + this.getResources()
              .getString(R.string.send))
          .setContentText(this.getResources().getString(R.string.preparingSend))
          .setSmallIcon(R.mipmap.ic_launcher);
    }
  }

  private void finishSendNotification(AndroidAppInfo androidAppInfo) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      if (mBuilderSend != null) {
        mBuilderSend = new NotificationCompat.Builder(this);
      }
      ((NotificationCompat.Builder) mBuilderSend).setContentText(
          this.getResources().getString(R.string.transfCompleted))
          .setSmallIcon(android.R.drawable.stat_sys_download_done)
          .setProgress(0, 0, false)
          .setAutoCancel(true);
      if (mNotifyManager == null) {
        mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
      }
      mNotifyManager.notify(androidAppInfo.getPackageName().hashCode(),
          ((NotificationCompat.Builder) mBuilderSend).build());
    }
  }

  private void showSendProgress(String sendingAppName, int actual, AndroidAppInfo androidAppInfo) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      ((NotificationCompat.Builder) mBuilderSend).setContentText(
          this.getResources().getString(R.string.sending) + " " + sendingAppName);
      ((NotificationCompat.Builder) mBuilderSend).setProgress(100, actual, false);
      if (mNotifyManager == null) {
        mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
      }
      mNotifyManager.notify(androidAppInfo.getPackageName().hashCode(),
          ((NotificationCompat.Builder) mBuilderSend).build());
    }
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null) {

      if (intent.getAction() != null && intent.getAction().equals("RECEIVE")) {
        final String externalStoragepath = intent.getStringExtra("ExternalStoragePath");

        System.out.println("Going to start serving");
        aptoideMessageServerSocket = new AptoideMessageServerSocket(55555, Integer.MAX_VALUE);
        aptoideMessageServerSocket.setHostsChangedCallbackCallback(new HostsChangedCallback() {
          @Override public void hostsChanged(List<Host> hostList) {
            System.out.println("hostsChanged() called with: " + "hostList = [" + hostList + "]");
            DataHolder.getInstance().setConnectedClients(hostList);
            Intent i = new Intent();
            if (hostList.size() >= 2) {
              System.out.println("sending broadcast of show_send_button");
              i.setAction("SHOW_SEND_BUTTON");
              sendBroadcast(i);
            } else {
              i.setAction("HIDE_SEND_BUTTON");
              sendBroadcast(i);
            }
          }
        });
        aptoideMessageServerSocket.startAsync();

        StorageCapacity storageCapacity = new StorageCapacity() {
          @Override public boolean hasCapacity(long bytes) {
            long availableSpace = -1L;
            StatFs stat = new StatFs(externalStoragepath);
            availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
            return availableSpace > bytes;
          }
        };

        // TODO: 22-02-2017 fix this hardcoded ip

        aptoideMessageClientSocket =
            new AptoideMessageClientSocket("192.168.43.1", 55555, externalStoragepath,
                storageCapacity, fileServerLifecycle, fileClientLifecycle);
        aptoideMessageClientSocket.startAsync();

        System.out.println("Connected 342");
      } else if (intent.getAction() != null && intent.getAction().equals("SEND")) {
        Bundle b = intent.getBundleExtra("bundle");

        listOfApps = b.getParcelableArrayList("listOfAppsToInstall");

        for (int i = 0; i < listOfApps.size(); i++) {
          String filePath = listOfApps.get(i).getFilePath();
          String appName = listOfApps.get(i).getAppName();
          String packageName = listOfApps.get(i).getPackageName();
          String obbsFilePath = listOfApps.get(i).getObbsFilePath();

          File apk = new File(filePath);

          File mainObb = null;
          File patchObb = null;

          List<FileInfo> fileInfoList = getFileInfo(filePath, obbsFilePath);

          final AndroidAppInfo appInfo = new AndroidAppInfo(appName, packageName, fileInfoList);

          AptoideUtils.ThreadU.runOnIoThread(new Runnable() {
            @Override public void run() {
              aptoideMessageClientSocket.send(
                  new RequestPermissionToSend(aptoideMessageClientSocket.getLocalhost(), appInfo));
            }
          });
        }
      } else if (intent.getAction() != null && intent.getAction().equals("SHUTDOWN_SERVER")) {
        aptoideMessageClientSocket.disable();
        if (aptoideMessageServerSocket != null) { // TODO: 16-03-2017 filipe check problem
          aptoideMessageClientSocket.disable();
          aptoideMessageServerSocket.shutdown(new Runnable() {
            @Override public void run() {
              setInitialApConfig();//to not interfere with recovering wifi state

              Intent i = new Intent();
              i.setAction("SERVER_DISCONNECT");
              sendBroadcast(i);
            }
          });
        }
      }
    }
    return START_STICKY;
  }

  /**
   * @deprecated Duplicated! {@link HighwayTransferRecordActivity#setInitialApConfig()}
   */
  @Deprecated public void setInitialApConfig() {
    WifiManager wifimanager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

    Method[] methods = wifimanager.getClass().getDeclaredMethods();
    WifiConfiguration wc = DataHolder.getInstance().getWcOnJoin();
    for (Method m : methods) {
      if (m.getName().equals("setWifiApConfiguration")) {

        try {
          Method setConfigMethod =
              wifimanager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
          System.out.println("Re-seting the wifiAp configuration to what it was before !!! ");
          setConfigMethod.invoke(wifimanager, wc);
        } catch (NoSuchMethodException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }
      if (m.getName().equals("setWifiApEnabled")) {

        try {
          System.out.println("Desligar o hostpot ");
          m.invoke(wifimanager, wc, false);
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }

  public List<FileInfo> getFileInfo(String filePath, String obbsFilePath) {
    List<FileInfo> fileInfoList = new ArrayList<>();
    File apk = new File(filePath);
    FileInfo apkFileInfo = new FileInfo(apk);
    fileInfoList.add(apkFileInfo);

    if (!obbsFilePath.equals("noObbs")) {
      File obbFolder = new File(obbsFilePath);
      File[] list = obbFolder.listFiles();
      if (list != null) {
        if (list.length > 0) {
          for (int i = 0; i < list.length; i++) {
            fileInfoList.add(new FileInfo(list[i]));
          }
        }
      }
    }

    return fileInfoList;
  }
}
