CREATE DATABASE IF NOT EXISTS xiuxian_game
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'xiuxian'@'localhost' IDENTIFIED BY 'xiuxian_dev';
CREATE USER IF NOT EXISTS 'xiuxian'@'127.0.0.1' IDENTIFIED BY 'xiuxian_dev';

GRANT ALL PRIVILEGES ON xiuxian_game.* TO 'xiuxian'@'localhost';
GRANT ALL PRIVILEGES ON xiuxian_game.* TO 'xiuxian'@'127.0.0.1';
FLUSH PRIVILEGES;

