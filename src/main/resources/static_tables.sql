-- Tạo bảng areas
CREATE TABLE IF NOT EXISTS areas (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- Tạo bảng provinces
CREATE TABLE IF NOT EXISTS provinces (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    area_code VARCHAR(10) NOT NULL,
    FOREIGN KEY (area_code) REFERENCES areas(code)
);

-- Tạo bảng districts
CREATE TABLE IF NOT EXISTS districts (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    province_code VARCHAR(10) NOT NULL,
    FOREIGN KEY (province_code) REFERENCES provinces(code)
);

-- Tạo bảng nations
CREATE TABLE IF NOT EXISTS nations (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- Tạo bảng main_stations
CREATE TABLE IF NOT EXISTS main_stations (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- Tạo bảng fields
CREATE TABLE IF NOT EXISTS fields (
    id INT AUTO_INCREMENT PRIMARY KEY,
    field VARCHAR(100) NOT NULL
);

-- Dữ liệu mẫu cho areas
INSERT IGNORE INTO areas (code, name) VALUES
('KV1', 'Khu vực 1'),
('KV2', 'Khu vực 2'),
('KV3', 'Khu vực 3');

-- Dữ liệu mẫu cho provinces
INSERT IGNORE INTO provinces (code, name, type, area_code) VALUES
('TTH', 'Thừa Thiên Huế', 'tỉnh', 'KV2'),
('KGG', 'Kiên Giang', 'tỉnh', 'KV3'),
('HPG', 'Hải Phòng', 'thành phố', 'KV1'),
('GLI', 'Gia Lai', 'tỉnh', 'KV2'),
('BDH', 'Bình Định', 'tỉnh', 'KV2'),
('AGG', 'An Giang', 'tỉnh', 'KV3'),
('NAN', 'Nghệ An', 'tỉnh', 'KV1'),
('BKN', 'Bắc Kạn', 'tỉnh', 'KV1'),
('QNH', 'Quảng Ninh', 'tỉnh', 'KV1'),
('BTE', 'Bến Tre', 'tỉnh', 'KV3'),
('QNI', 'Quảng Ngãi', 'tỉnh', 'KV2'),
('HNI', 'Hà Nội', 'thành phố', 'KV1'),
('DNI', 'Đồng Nai', 'tỉnh', 'KV3'),
('DLK', 'Đắk Lắk', 'tỉnh', 'KV2');

-- Dữ liệu mẫu cho districts
INSERT IGNORE INTO districts (code, name, type, province_code) VALUES
('T054008', 'A Lưới', 'huyện', 'TTH'),
('K077009', 'An Biên', 'huyện', 'KGG'),
('H031008', 'An Dương', 'huyện', 'HPG'),
('H031015', 'An Hải', 'huyện', 'HPG'),
('G059007', 'An Khê', 'huyện', 'GLI'),
('H031009', 'An Lão', 'huyện', 'HPG'),
('B056002', 'An Lão', 'huyện', 'BDH'),
('K077010', 'An Minh', 'huyện', 'KGG'),
('B056009', 'An Nhơn', 'huyện', 'BDH'),
('A076003', 'An Phú', 'huyện', 'AGG'),
('N038014', 'Anh Sơn', 'huyện', 'NAN'),
('G059012', 'Ayun Pa', 'huyện', 'GLI'),
('B281002', 'Ba Bể', 'huyện', 'BKN'),
('Q033009', 'Ba Chẽ', 'huyện', 'QNH'),
('B075007', 'Ba Tri', 'quận', 'BTE'),
('Q055013', 'Ba Tơ', 'huyện', 'QNI'),
('H004017', 'Ba Vì', 'huyện', 'HNI'),
('H004003', 'Ba Đình', 'quận', 'HNI'),
('D061001', 'Biên Hòa', 'thành phố', 'DNI'),
('D500015', 'Buôn Hồ', 'huyện', 'DLK'),
('D500001', 'Buôn Ma Thuột', 'thành phố', 'DLK'),
('D500006', 'Buôn Đôn', 'huyện', 'DLK');

-- Dữ liệu mẫu cho nations
INSERT IGNORE INTO nations (code, name) VALUES
('VN', 'Việt Nam, Vietnam, VNI'),
('STL', 'Lào, Laos, Lao'),
('MYT', 'Myanmar, Myanmar');

-- Dữ liệu mẫu cho main_stations
INSERT IGNORE INTO main_stations (code, name) VALUES
('GM', 'Giang Văn Minh (GVM)'),
('HT', 'Hoàng Hoa Thám (HHT)'),
('PV', 'Pháp Vân (PVN)');

-- Dữ liệu mẫu cho fields
INSERT IGNORE INTO fields (id, field) VALUES
(1, 'Vô tuyến'),
(2, 'Truyền dẫn'),
(3, 'Cơ điện'),
(4, 'Mạng lõi'),
(5, 'BRCĐ');

-- Tạo bảng agents
CREATE TABLE IF NOT EXISTS agents (
    id INT AUTO_INCREMENT PRIMARY KEY,
    agent VARCHAR(100) NOT NULL
);

-- Dữ liệu mẫu cho agents
INSERT IGNORE INTO agents (id, agent) VALUES
(1, 'FUNCTION_CALL'),
(2, 'DOC_SEARCH'),
(3, 'NEW_SEARCH'); 