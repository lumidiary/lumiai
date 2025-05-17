package com.example.insightservice.util;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import java.io.IOException;

public class ImageCompressor {

    private static final int QUALITY = 50;

    // 이미지의 긴 쪽 최대 크기를 384px로 리사이즈하고, webp로 변환
    public static byte[] compressImage(byte[] inputBytes) {
        try {
            ImmutableImage originalImage = ImmutableImage.loader().fromBytes(inputBytes);
            int originalWidth = originalImage.width;
            int originalHeight = originalImage.height;
            int maxDim = Math.max(originalWidth, originalHeight);
            double scale = (maxDim > 384) ? 384.0 / maxDim : 1.0;
            
            ImmutableImage resizedImage = originalImage.scale(scale);
            return resizedImage.bytes(WebpWriter.DEFAULT.withQ(QUALITY));
        } catch (IOException e) {
            throw new RuntimeException("이미지 압축 중 에러 발생", e);
        }
    }
}
