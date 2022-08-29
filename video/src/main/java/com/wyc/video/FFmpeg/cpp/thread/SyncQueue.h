//
// Created by Administrator on 2022/8/29.
//
#include "../utils/MacroUtil.h"
#include <mutex>
#include <condition_variable>
#include <thread>
#include <queue>

using namespace std;

#ifndef ANDROIDFEATURESET_SYNCQUEUE_H
#define ANDROIDFEATURESET_SYNCQUEUE_H

template<typename T>
class SyncQueue final {
public:
    DISABLE_COPY_ASSIGN(SyncQueue)
    SyncQueue();
    ~SyncQueue();
    bool push(T &o);
    bool push(T &&o);
    bool take(T &o);
    bool take(T &o,long millisecond);
    int size(){
        std::lock_guard<mutex> lockGuard(m_mutex);
        return m_queue.size();
    }
private:
    bool empty(){
        return m_queue.empty();
    }
    void clear(){
        std::lock_guard<mutex> lockGuard(m_mutex);
        while (!m_queue.empty())m_queue.pop();
        isClear = true;
        m_cond_not_empty.notify_all();
    }
private:
    bool volatile isClear = false;
    mutex m_mutex;
    condition_variable_any m_cond_not_empty;
    queue<T> m_queue;
};



#include "../utils/LogUtil.h"

template<typename T>
SyncQueue<T>::SyncQueue() {
    LOGD("SyncQueue construction");
}

template<typename T>
SyncQueue<T>::~SyncQueue() {
    LOGD("SyncQueue destruction");
    clear();
    LOGD("SyncQueue size:%d",m_queue.size());
}

template<typename T>
bool SyncQueue<T>::push(T &o) {
    std::lock_guard<mutex> lockGuard(m_mutex);

    if (isClear)return false;

    m_queue.push(o);
    m_cond_not_empty.notify_all();

    return true;
}

template<typename T>
bool SyncQueue<T>::push(T &&o) {
    std::lock_guard<mutex> lockGuard(m_mutex);

    if (isClear)return false;

    m_queue.push(o);
    m_cond_not_empty.notify_all();

    return true;
}

template<typename T>
bool SyncQueue<T>::take(T &o) {
    std::lock_guard<mutex> lockGuard(m_mutex);
    if (isClear){
        return false;
    }
    m_cond_not_empty.wait(m_mutex,[this]{ return !m_queue.empty();});

    o =  m_queue.front();
    m_queue.pop();
    return true;
}
template<typename T>
bool SyncQueue<T>::take(T &o,long millisecond){
    std::lock_guard<mutex> lockGuard(m_mutex);
    if (isClear){
        return false;
    }
    if (empty()){
        if (m_cond_not_empty.wait_for(m_mutex,chrono::milliseconds(millisecond)) == std::cv_status::timeout){
            LOGE("SyncQueue take timeout");
            return false;
        }
    }
    o =  m_queue.front();
    m_queue.pop();
    return true;
}

#endif //ANDROIDFEATURESET_SYNCQUEUE_H