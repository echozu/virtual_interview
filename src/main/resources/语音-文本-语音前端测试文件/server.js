const http = require('http');
const fs = require('fs');
const path = require('path');

const server = http.createServer((req, res) => {
    let filePath = '.' + req.url;
    
    // 默认提供 index.html
    if (filePath === './') {
        filePath = './websocket-audio-test.html';
    }
    
    // 获取文件扩展名
    const extname = path.extname(filePath);
    let contentType = 'text/html';
    
    // 设置正确的 MIME 类型
    switch (extname) {
        case '.js':
            contentType = 'text/javascript';
            break;
        case '.css':
            contentType = 'text/css';
            break;
        case '.json':
            contentType = 'application/json';
            break;
        case '.png':
            contentType = 'image/png';
            break;
        case '.jpg':
            contentType = 'image/jpg';
            break;
        case '.wav':
            contentType = 'audio/wav';
            break;
        case '.svg':
            contentType = 'image/svg+xml';
            break;
    }
    
    // 读取文件
    fs.readFile(filePath, (error, content) => {
        if (error) {
            if (error.code === 'ENOENT') {
                res.writeHead(404);
                res.end('File not found');
            } else {
                res.writeHead(500);
                res.end('Server error: ' + error.code);
            }
        } else {
            res.writeHead(200, { 'Content-Type': contentType });
            res.end(content, 'utf-8');
        }
    });
});

const PORT = 3000;
server.listen(PORT, () => {
    console.log(`服务器运行在 http://localhost:${PORT}`);
    console.log('请访问: http://localhost:3000/websocket-audio-test.html');
}); 