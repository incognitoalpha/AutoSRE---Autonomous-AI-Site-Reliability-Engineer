package com.autosre.anomaly.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for anomaly detection thresholds and parameters.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 */
@Configuration
@ConfigurationProperties(prefix = "autosre.detection")
public class DetectionConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DetectionConfig.class);

    private ZScoreConfig zscore = new ZScoreConfig();
    private MadConfig mad = new MadConfig();

    public ZScoreConfig getZscore() {
        return zscore;
    }

    public void setZscore(ZScoreConfig zscore) {
        this.zscore = zscore;
    }

    public MadConfig getMad() {
        return mad;
    }

    public void setMad(MadConfig mad) {
        this.mad = mad;
    }

    public static class ZScoreConfig {
        private double threshold = 3.0;
        private int minBaselineSize = 30;

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        public int getMinBaselineSize() {
            return minBaselineSize;
        }

        public void setMinBaselineSize(int minBaselineSize) {
            this.minBaselineSize = minBaselineSize;
        }
    }

    public static class MadConfig {
        private double threshold = 3.5;
        private double sensitivity = 1.4826;
        private int minBaselineSize = 30;

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        public double getSensitivity() {
            return sensitivity;
        }

        public void setSensitivity(double sensitivity) {
            this.sensitivity = sensitivity;
        }

        public int getMinBaselineSize() {
            return minBaselineSize;
        }

        public void setMinBaselineSize(int minBaselineSize) {
            this.minBaselineSize = minBaselineSize;
        }
    }
}