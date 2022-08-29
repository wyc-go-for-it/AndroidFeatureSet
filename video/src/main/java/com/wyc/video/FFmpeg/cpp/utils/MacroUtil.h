//
// Created by Administrator on 2022/8/29.
//

#ifndef ANDROIDFEATURESET_MACROUTIL_H
#define ANDROIDFEATURESET_MACROUTIL_H

#define DISABLE_COPY_ASSIGN(cls)  \
    cls(const cls &o) = delete; \
    cls &operator =(const cls &o) = delete; \
    cls(const cls &&o) = delete; \
    cls &operator =(const cls &&o) = delete; \

#endif //ANDROIDFEATURESET_MACROUTIL_H
