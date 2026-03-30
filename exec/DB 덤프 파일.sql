CREATE DATABASE kairos_db;
USE kairos_db;

INSERT INTO dev_position (position_name) VALUES
('Backend Developer'),
('Frontend Developer'),
('Fullstack Developer'),
('Mobile Developer'),
('DevOps Engineer'),
('Data Engineer'),
('AI/ML Engineer'),
('QA Engineer'),
('Security Engineer'),
('Game Developer');

INSERT INTO tech_stack (tech_name, icon_url, color) VALUES
('Java', 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/java/java-original.svg', '#007396'),
('Spring Boot', 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg', '#6DB33F'),
('JavaScript', 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/javascript/javascript-original.svg', '#F7DF1E'),
('TypeScript', 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/typescript/typescript-original.svg', '#3178C6'),
('React', 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/react/react-original.svg', '#61DAFB'),
('Vue.js', 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/vuejs/vuejs-original.svg', '#4FC08D'),
('Node.js', 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/nodejs/nodejs-original.svg', '#339933'),
('Python', 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/python/python-original.svg', '#3776AB'),
('Docker', 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/docker/docker-original.svg', '#2496ED'),
('AWS', 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/amazonwebservices/amazonwebservices-original-wordmark.svg', '#FF9900');