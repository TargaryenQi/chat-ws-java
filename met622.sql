/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50721
 Source Host           : localhost:3306
 Source Schema         : met622

 Target Server Type    : MySQL
 Target Server Version : 50721
 File Encoding         : 65001

 Date: 02/12/2019 22:39:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for activity
-- ----------------------------
DROP TABLE IF EXISTS `activity`;
CREATE TABLE `activity` (
  `start_time` time DEFAULT NULL,
  `end_time` time DEFAULT NULL,
  `activity` varchar(255) DEFAULT NULL,
  `duration` int(10) DEFAULT NULL,
  `date` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for activity_step
-- ----------------------------
DROP TABLE IF EXISTS `activity_step`;
CREATE TABLE `activity_step` (
  `date` date DEFAULT NULL,
  `step_counts` int(10) DEFAULT NULL,
  `step_delta` int(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for heart_rate
-- ----------------------------
DROP TABLE IF EXISTS `heart_rate`;
CREATE TABLE `heart_rate` (
  `date` date DEFAULT NULL,
  `bpm` int(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
