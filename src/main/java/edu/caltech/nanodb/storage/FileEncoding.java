package edu.caltech.nanodb.storage;

public enum FileEncoding {
    // RLE算法实现数据压缩,游程编码（Run-Length Encoding, RLE）又称行程长度编码或者变动长度编码法
    RLE,
    // 字典压缩
    DICTIONARY,
    // 无压缩
    NONE
}