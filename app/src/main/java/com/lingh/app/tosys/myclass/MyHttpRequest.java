package com.lingh.app.tosys.myclass;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.HTTP;

public interface MyHttpRequest {
    @HTTP(method = "GET", path = "https://api.github.com/repos/LGH1996/ToSysRelease/releases/latest")
    Observable<MyUpdateMessage> getUpdateMessage();
}
