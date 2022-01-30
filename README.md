# Milktea
<img src="https://github.com/Kinoshita0623/MisskeyAndroidClient/blob/master/app/src/main/ic_launcher-web.png?raw=true" width="100px">
MisskeyのAndroidクライアント

## 説明
MisskeyにMilkteaはいかが？<br>
これはMisskeyのAndroidのクライアント

## インストール方法
[GooglePlayストア](https://play.google.com/store/apps/details?id=jp.panta.misskeyandroidclient)でダウンロード&インストール

利用するインスタンスで事前にアカウントを作成してください。<br>
[はじめに](https://join.misskey.page/ja/wiki/first)
[インスタンス一覧](https://join.misskey.page/ja/wiki/instances/)

インストールが完了したらアプリを起動します。
認可画面が表示されるので、利用しようとしているインスタンスのURLを入力します。<br>
例えばmisskey.ioを利用する場合は、「misskey.io」と入力します。

app nameは自由に設定することがでます。<br>
app nameはインスタンスのバージョンによってはvia名として公開される場合があります。<br>

準備ができれば AUTHENTICATION (認証)を押します。<br>
<img src="https://user-images.githubusercontent.com/38454985/81928170-d03c8080-961f-11ea-8acc-b1d752d72de7.png" width="320px">

認証画面がブラウザに表示されるので、問題がなければ許可(Accept)を押します。<br>
もし、リダイレクトしない場合は戻るボタンを押して、「私は許可をしました」を押してください。
<img src="https://user-images.githubusercontent.com/38454985/81928454-3cb77f80-9620-11ea-839b-ea28962a0a92.png" width="320px">

成功すればMilkteaにリダイレクトするので[続行(CONTINUE)]を押し完了です。<br>
<img src="https://user-images.githubusercontent.com/38454985/81928572-6c668780-9620-11ea-800a-bbb03721ce8e.png" width="320px">



## ビルドするには

プロジェクトをgit cloneします。<br>
local.propertiesを作成します。<br>
```
touch local.properties
```
local.propertiesには<br>
以下のような属性を追加してプッシュ通知の中継鯖についての設定をします。<br>
プッシュ通知中継サーバについて<br>
https://github.com/pantasystem/MisskeyAndroidClient/blob/develop/PushToFCM/README.md<br>

push_to_fcm.server_base_urlにはプッシュ通知サーバのベースURLを設定します。<br>
push_to_fcm.public_keyにはPushToFCMで生成したpublicを設定します。<br>
push_to_fcm.authにはPushToFCMで生成したauthを設定します。

```
push_to_fcm.server_base_url=https://hogehogehoge-pus
push_to_fcm.public_key=中継鯖（PushToFCM）に設定したpublic_keyを設定します
push_to_fcm.auth=中継鯖に設定したauth_secret.txtを設定します
```
Android SDK, AndroidStudioでビルドします。
