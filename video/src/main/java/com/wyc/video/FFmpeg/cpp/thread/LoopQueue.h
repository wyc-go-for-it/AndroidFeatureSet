//
// Created by Administrator on 2022-09-17.
//

#ifndef ANDROIDFEATURESET_LOOPQUEUE_H
#define ANDROIDFEATURESET_LOOPQUEUE_H

#include "../utils/MacroUtil.h"
#include <mutex>
#include <condition_variable>
#include <thread>
#include <queue>
#include <type_traits>
#include "../utils/LogUtil.h"

using namespace std;


template<typename T,__int32_t s = 1024>
class LoopQueue final {
public:
    DISABLE_COPY_ASSIGN(LoopQueue)
    LoopQueue();
    ~LoopQueue();
    void push(T &o);
    void push(T &&o);
    bool take(T &o);
    int size() const {
        return m_queue.size();
    }
    int curWriteIndex() const{
        return mWriteIndex;
    }
    int curReadIndex() const{
        return mReadIndex;
    }
    bool isFull() const{
        return mFull;
    }
    bool isTail() const{
        return mReadIndex == mWriteIndex;
    }
    void clear();
private:
    bool empty() const{
        return mWriteIndex == mReadIndex == 0;
    }
private:
    std::atomic<int32_t> mWriteIndex { 0 };
    std::atomic<int32_t> mReadIndex { 0 };
    std::atomic<bool> mFull { false };

    std::array<T,s> m_queue;
};

template<typename T,int s>
LoopQueue<T,s>::LoopQueue(){
}

template<typename T,int s>
LoopQueue<T,s>::~LoopQueue() {

}

template<typename T,int s>
void LoopQueue<T,s>::push(T &o) {
    if (mWriteIndex >= s){
        mWriteIndex = 0;
        if (!mFull){
            mFull = true;
        }
    }

    m_queue[mWriteIndex++] = o;
}

template<typename T,int s>
void LoopQueue<T,s>::push(T &&o) {
    if (mWriteIndex >= s){
        mWriteIndex = 0;
        if (!mFull){
            mFull = true;
        }
    }
    m_queue[mWriteIndex++] = o;
}

template<typename T,int s>
bool LoopQueue<T,s>::take(T &o) {
    if (mFull){
        if (mReadIndex >= s){
            mReadIndex = 0;
        }
    } else{
        if (mReadIndex >= mWriteIndex){
            mReadIndex = 0;
        }
    }
    o =  m_queue[mReadIndex++];

    return true;
}

template<typename T,int s>
void LoopQueue<T,s>::clear(){
    mWriteIndex = mReadIndex = 0;
    mFull = false;
    LOGD("clear LoopQueue WriteIndex:%d,ReadIndex:%d",mWriteIndex.load(),mReadIndex.load());
}

#endif //ANDROIDFEATURESET_LOOPQUEUE_H
