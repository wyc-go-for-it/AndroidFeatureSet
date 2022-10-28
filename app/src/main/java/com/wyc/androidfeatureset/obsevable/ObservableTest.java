package com.wyc.androidfeatureset.obsevable;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.androidfeatureset.obsevable
 * @ClassName: Observable
 * @Description: 作用描述
 * @Author: wyc
 * @CreateDate: 2022/10/27 17:54
 * @UpdateUser: 更新者：
 * @UpdateDate: 2022/10/27 17:54
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class ObservableTest {


    public static void main(String[] args){

        save1();
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void save1(){

        Observable<Boolean> first =  Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@io.reactivex.rxjava3.annotations.NonNull ObservableEmitter<Boolean> emitter) throws Throwable {

                emitter.onNext(true);
            }
        });

        Observable<Boolean> second =  Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@io.reactivex.rxjava3.annotations.NonNull ObservableEmitter<Boolean> emitter) throws Throwable {

                System.out.println("second cur thread:" + Thread.currentThread().getName());
                emitter.onNext(true);
            }
        });

        Observable.fromArray(first,second).all(booleanObservable -> {
            System.out.println(booleanObservable.toString()+"-------"+ booleanObservable.blockingFirst());
            return booleanObservable.blockingFirst();
        }).subscribe(new SingleObserver<Boolean>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onSuccess(@NonNull Boolean aBoolean) {
                System.out.println(aBoolean);
            }

            @Override
            public void onError(@NonNull Throwable e) {

            }
        });
    }
}
