Các bước cần có để thực hiện quá trình kiểm duyệt tự động (DAST) bằng công cụ OWASP-ZAP:

B1: Cài đặt Dockerfile đã được đính kèm (Dockerfile không có đuôi) ngang hàng với các thư mục chính như database, src, target, uploads...
B2: Nếu muốn sử dụng người dùng phải tạo 1 repo trên github đặt tên trang web mà mọi người sử dụng sau đó cần phải remote về máy.
B3: Lệnh remote về máy theo cấu trúc sau: git remote add origin https://github.com/user/"tên repo của bạn".
B4: Tạo 1 thư mục .github\workflows: Sau đó cài 2 file dast.yml và dast_full.yml tại đây.
B5: Nếu chưa có git thì cần phải cài đặt git sau đó khởi tạo repo cho git(git init).
B6: Tạo 1 nhánh mới k nên push lên main để dễ dàng chỉnh sửa nếu phát hiện ra lỗ hổng..
B7: Sau đó các khối lệnh sau để tải lên github để sử dụng ZAP:
                git branch "tên nhanh bạn vừa tạo"
                git add .
                git commit -m "Tên quá trình b muốn đẩy lên"
                git push -u origin "tên nhánh của bạn"
B8: Vào phần Actions của repo github bạn sẽ thấy được quy trình hoạt động của OWASP-ZAP.

