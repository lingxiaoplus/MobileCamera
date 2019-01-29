package com.media.lingxiao.harddecoder;

import android.util.Log;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class IoResultFunction<T> implements Function<Throwable,Observable<T>> {
    private static final String TAG = IoResultFunction.class.getSimpleName();
    public static final int UN_KNOWN_ERROR = 1000;//未知错误
    @Override
    public Observable<T> apply(Throwable throwable) throws Exception {
        Log.e(TAG, "error: "+throwable.getMessage());
        ApiException ex = new ApiException(throwable, UN_KNOWN_ERROR);
        return Observable.error(throwable);
    }
}
