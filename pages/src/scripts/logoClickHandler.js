// 添加一个处理 logo 点击事件的脚本
document.addEventListener('DOMContentLoaded', function() {
    // 查找页面中的 logo SVG 元素
    // 更精确地选择 hero 区域的图片
    const logoImg = document.querySelector('.hero img');
    const logoSvg = document.querySelector('.hero svg');
    const logoElement = logoImg || logoSvg;
    
    // 如果找到了 logo 元素，则添加点击事件监听器
    if (logoElement) {
        logoElement.style.cursor = 'pointer';
        
        logoElement.addEventListener('click', function(event) {
            event.preventDefault();
            
            // 发起 POST 请求
            fetch('/api/logo-click', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    timestamp: new Date().toISOString(),
                    userAgent: navigator.userAgent,
                    url: window.location.href,
                    // 可以添加更多需要发送的数据
                })
            })
            .then(response => {
                if (response.ok) {
                    console.log('Logo click recorded successfully');
                    // 可选：显示一个反馈给用户
                    alert('感谢您的关注！');
                } else {
                    console.error('Failed to record logo click');
                }
            })
            .catch(error => {
                console.error('Error:', error);
            });
        });
    }
});