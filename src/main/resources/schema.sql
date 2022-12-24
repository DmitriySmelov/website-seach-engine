-- MySQL Distrib 8.0.29, for Windows (x86_64)
--
-- Host: localhost    Database: search_engine
--------------------------------------------------------
--
-- Table structure for table `sites`
--

 DROP TABLE IF EXISTS `sites`;

 CREATE TABLE `sites` (
`id` INT NOT NUll AUTO_INCREMENT PRIMARY KEY,
`status` ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL,
`status_time` DATETIME NOT NULL,
`last_error` TEXT,
`url` VARCHAR(255) NOT NULL,
`name` VARCHAR(255) NOT NULL
);

--
-- Table structure for table `pages`
--

DROP TABLE IF EXISTS `pages`;

 CREATE TABLE `pages` (
`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
`site_id` INT NOT NULL,
`path` TEXT NOT NULL ,
`code` INT NOT NULL,
`content` MEDIUMTEXT NOT NULL,
FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`)
ON UPDATE CASCADE
ON DELETE CASCADE,
index (`path`(250))
);

--
-- Table structure for table `lemmas`
--

 DROP TABLE IF EXISTS `lemmas`;

  CREATE TABLE `lemmas` (
 `id` INT NOT NULL AUTO_INCREMENT unique,
 `site_id` INT NOT NULL,
 `lemma` VARCHAR(255) NOT NULL,
 `frequency` INT NOT NULL,
 CONSTRAINT `id` PRIMARY KEY CLUSTERED (`lemma` ASC, `site_id` ASC),
 FOREIGN KEY (`site_id`) REFERENCES `sites` (`id`)
 ON UPDATE CASCADE
 ON DELETE CASCADE,
 index lemma(30)
);

--
-- Table structure for table `indexes`
--

DROP TABLE IF EXISTS `indexes`;

CREATE TABLE `indexes` (
`id` INT NOT NULL AUTO_INCREMENT unique,
`page_id` INT NOT NULL,
`lemma_id` INT NOT NULL,
`grade` FLOAT NOT NULL,
CONSTRAINT `id` PRIMARY KEY CLUSTERED (`page_id` ASC, `lemma_id` ASC) ,
FOREIGN KEY (`page_id`) REFERENCES `pages` (`id`)
ON UPDATE CASCADE
ON DELETE CASCADE,
FOREIGN KEY (`lemma_id`) REFERENCES `lemmas` (`id`)
ON UPDATE CASCADE
ON DELETE CASCADE
);
