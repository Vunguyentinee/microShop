package com.microshop.controller;

import com.microshop.context.DBContext;
import com.microshop.dao.AnhTaiKhoanDAO;
import com.microshop.dao.DanhMucDAO;
import com.microshop.dao.TaiKhoanDAO;
import com.microshop.dao.TaiKhoanFreeFireDAO;
import com.microshop.dao.TaiKhoanLienQuanDAO;
import com.microshop.dao.TaiKhoanRiotDAO;
import com.microshop.model.AnhTaiKhoan;
import com.microshop.model.DanhMuc;
import com.microshop.model.TaiKhoan;
import com.microshop.model.TaiKhoanFreeFire;
import com.microshop.model.TaiKhoanLienQuan;
import com.microshop.model.TaiKhoanRiot;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.nio.file.Paths;
import java.util.ArrayList; 
import java.util.Collection; 
import java.util.List;
import java.util.Map;
import java.util.UUID; 
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
        maxFileSize = 1024 * 1024 * 10, // 10 MB
        maxRequestSize = 1024 * 1024 * 50 // 50 MB (cho nhiều ảnh)
)
@WebServlet(name = "AdminGameAccountServlet", urlPatterns = {
    "/admin/products/game", // GET
    "/admin/account/add",   // GET
    "/admin/account/edit",  // GET
    "/admin/account/save",  // POST
    "/admin/account/delete" // POST
})
public class AdminGameAccountServlet extends HttpServlet {

    private TaiKhoanDAO taiKhoanDAO;
    private DanhMucDAO danhMucDAO;
    private TaiKhoanLienQuanDAO lienQuanDAO;
    private TaiKhoanFreeFireDAO freeFireDAO;
    private TaiKhoanRiotDAO riotDAO;
    private AnhTaiKhoanDAO anhTaiKhoanDAO;

    private Map<Integer, String> categoryMap;

    private final String EXTERNAL_UPLOAD_PATH = "/uploads";

    @Override
    public void init() {
        taiKhoanDAO = new TaiKhoanDAO();
        danhMucDAO = new DanhMucDAO();
        lienQuanDAO = new TaiKhoanLienQuanDAO();
        freeFireDAO = new TaiKhoanFreeFireDAO();
        riotDAO = new TaiKhoanRiotDAO();
        anhTaiKhoanDAO = new AnhTaiKhoanDAO();

        try {
            categoryMap = danhMucDAO.getAll().stream()
                    .collect(Collectors.toMap(DanhMuc::getMaDanhMuc, DanhMuc::getTenDanhMuc));
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tải Danh Mục!", e);
        }

        File uploadDir = new File(EXTERNAL_UPLOAD_PATH);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getServletPath();
        try {
            switch (action) {
                case "/admin/account/add":
                    showNewForm(request, response);
                    break;
                case "/admin/account/edit":
                    showEditForm(request, response);
                    break;
                default:
                    listGameAccounts(request, response);
                    break;
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getServletPath();

        try {
            switch (action) {
                case "/admin/account/save":
                    saveGameAccount(request, response);
                    break;
                case "/admin/account/delete":
                    deleteGameAccount(request, response);
                    break;
            }
        } catch (Exception e) {
            Logger.getLogger(AdminGameAccountServlet.class.getName()).log(Level.SEVERE, "Lỗi doPost", e);
            request.setAttribute("errorMessage", "Lỗi nghiêm trọng: " + e.getMessage());
            try {
                listGameAccounts(request, response);
            } catch (SQLException ex) {
                System.getLogger(AdminGameAccountServlet.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
    }

    private void listGameAccounts(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        int page = 1;
        int recordsPerPage = 15;
        if (request.getParameter("page") != null) {
            page = Integer.parseInt(request.getParameter("page"));
        }
        List<TaiKhoan> listAccounts = taiKhoanDAO.getAllPaginated(page, recordsPerPage);
        int noOfRecords = taiKhoanDAO.getTotalCount();
        int noOfPages = (int) Math.ceil(noOfRecords * 1.0 / recordsPerPage);
        request.setAttribute("listAccounts", listAccounts);
        request.setAttribute("categoryMap", categoryMap);
        request.setAttribute("noOfPages", noOfPages);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalRecords", noOfRecords);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/admin/manage_game_accounts.jsp");
        dispatcher.forward(request, response);
    }

    private void deleteGameAccount(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int id = Integer.parseInt(request.getParameter("id"));

        TaiKhoan tk = taiKhoanDAO.getById(id);
        if (tk != null && tk.getDuongDanAnh() != null) {
            deleteFile(tk.getDuongDanAnh());
        }
        List<AnhTaiKhoan> listAnh = anhTaiKhoanDAO.getByMaTaiKhoan(id);
        if (listAnh != null) {
            for (AnhTaiKhoan anh : listAnh) {
                deleteFile(anh.getDuongDanAnh());
            }
        }

        taiKhoanDAO.delete(id);
        response.sendRedirect(request.getContextPath() + "/admin/products/game");
    }

    private void showNewForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("taiKhoan", new TaiKhoan());
        request.setAttribute("danhMucMap", categoryMap);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/admin/form_game_account.jsp");
        dispatcher.forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        int id = Integer.parseInt(request.getParameter("id"));
        TaiKhoan taiKhoan = taiKhoanDAO.getById(id);
        if (taiKhoan == null) {
            response.sendRedirect(request.getContextPath() + "/admin/products/game");
            return;
        }

        TaiKhoan taiKhoanFull = null;
        int maDanhMuc = taiKhoan.getMaDanhMuc();

        if (maDanhMuc == 1) { // Free Fire
            taiKhoanFull = freeFireDAO.getById(id);
        } else if (maDanhMuc == 2) { // Liên Quân
            taiKhoanFull = lienQuanDAO.getById(id);
        } else if (maDanhMuc == 3) { // Riot
            taiKhoanFull = riotDAO.getById(id);
        } else { 
            taiKhoanFull = taiKhoan; // Trường hợp danh mục lạ ( đề phòng lỗi quỷ )
        }

        request.setAttribute("taiKhoan", taiKhoanFull);
        request.setAttribute("danhMucMap", categoryMap);

        RequestDispatcher dispatcher = request.getRequestDispatcher("/admin/form_game_account.jsp");
        dispatcher.forward(request, response);
    }

    private void saveGameAccount(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException, ServletException {

        Connection conn = null;
        String newAnhChinhPath = null;
        String anhHienTai = request.getParameter("anhHienTai");
        List<String> newAnhPhuPaths = new ArrayList<>();

        try {
            // 1. Xử lý Upload Ảnh Chính
            Part fileAnhChinh = request.getPart("fileAnhChinh");
            if (fileAnhChinh != null && fileAnhChinh.getSize() > 0) {
                newAnhChinhPath = saveFile(fileAnhChinh);
                if (anhHienTai != null && !anhHienTai.isEmpty()) {
                    deleteFile(anhHienTai);
                }
            } else {
                newAnhChinhPath = anhHienTai;
            }

            // 2. Xử lý Upload Nhiều Ảnh Chi Tiết
            Collection<Part> fileParts = request.getParts().stream()
                    .filter(part -> "fileAnhPhu".equals(part.getName()) && part.getSize() > 0)
                    .collect(Collectors.toList());

            for (Part filePart : fileParts) {
                newAnhPhuPaths.add(saveFile(filePart));
            }

            // 3. Bắt đầu Transaction
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            // 4. Lấy dữ liệu chung (Bảng Cha)
            String maTaiKhoanParam = request.getParameter("maTaiKhoan");
            Integer maTaiKhoan = (maTaiKhoanParam == null || maTaiKhoanParam.isEmpty()) ? null : Integer.parseInt(maTaiKhoanParam);
            int maDanhMuc = Integer.parseInt(request.getParameter("maDanhMuc"));
            BigDecimal giaGoc = new BigDecimal(request.getParameter("giaGoc"));
            BigDecimal giaBan = new BigDecimal(request.getParameter("giaBan"));
            String trangThai = request.getParameter("trangThai");
            String diemNoiBat = request.getParameter("diemNoiBat");

            // 5. Lấy dữ liệu riêng (Bảng Con) - Tùy theo maDanhMuc
            Integer taiKhoanIdToUse = null; // ID của tài khoản (cũ hoặc mới)

            switch (maDanhMuc) {
                case 1: // Free Fire
                    TaiKhoanFreeFire ff = new TaiKhoanFreeFire();
                    // Set info cha
                    ff.setMaDanhMuc(maDanhMuc);
                    ff.setGiaGoc(giaGoc);
                    ff.setGiaBan(giaBan);
                    ff.setTrangThai(trangThai);
                    ff.setDiemNoiBat(diemNoiBat);
                    ff.setDuongDanAnh(newAnhChinhPath);
                    // Set info con
                    ff.setTenDangNhap(request.getParameter("ff_tenDangNhap"));
                    ff.setMatKhau(request.getParameter("ff_matKhau"));
                    ff.setHangRank(request.getParameter("ff_hangRank"));
                    ff.setSoSkinSung(Integer.parseInt(request.getParameter("ff_soSkinSung")));
                    ff.setLoaiDangKy(request.getParameter("ff_loaiDangKy"));
                    ff.setCoTheVoCuc(Boolean.parseBoolean(request.getParameter("ff_coTheVoCuc")));

                    if (maTaiKhoan == null) { // Thêm mới
                        taiKhoanIdToUse = freeFireDAO.insert(ff, conn);
                    } else { // Cập nhật
                        ff.setMaTaiKhoan(maTaiKhoan);
                        freeFireDAO.update(ff, conn);
                        taiKhoanIdToUse = maTaiKhoan;
                    }
                    break;

                case 2: // Liên Quân
                    TaiKhoanLienQuan lq = new TaiKhoanLienQuan();
                    // Set info cha
                    lq.setMaDanhMuc(maDanhMuc);
                    lq.setGiaGoc(giaGoc);
                    lq.setGiaBan(giaBan);
                    lq.setTrangThai(trangThai);
                    lq.setDiemNoiBat(diemNoiBat);
                    lq.setDuongDanAnh(newAnhChinhPath);
                    // Set info con
                    lq.setTenDangNhap(request.getParameter("lq_tenDangNhap"));
                    lq.setMatKhau(request.getParameter("lq_matKhau"));
                    lq.setHangRank(request.getParameter("lq_hangRank"));
                    lq.setSoTuong(Integer.parseInt(request.getParameter("lq_soTuong")));
                    lq.setSoTrangPhuc(Integer.parseInt(request.getParameter("lq_soTrangPhuc")));
                    lq.setLoaiDangKy(request.getParameter("lq_loaiDangKy"));
                    // (Thêm các trường khác nếu có)

                    if (maTaiKhoan == null) {
                        taiKhoanIdToUse = lienQuanDAO.insert(lq, conn);
                    } else {
                        lq.setMaTaiKhoan(maTaiKhoan);
                        lienQuanDAO.update(lq, conn);
                        taiKhoanIdToUse = maTaiKhoan;
                    }
                    break;

                case 3: // Riot
                    TaiKhoanRiot riot = new TaiKhoanRiot();
                    // Set info cha
                    riot.setMaDanhMuc(maDanhMuc);
                    riot.setGiaGoc(giaGoc);
                    riot.setGiaBan(giaBan);
                    riot.setTrangThai(trangThai);
                    riot.setDiemNoiBat(diemNoiBat);
                    riot.setDuongDanAnh(newAnhChinhPath);
                    // Set info con
                    riot.setTenDangNhap(request.getParameter("riot_tenDangNhap"));
                    riot.setMatKhau(request.getParameter("riot_matKhau"));
                    riot.setCapDoRiot(Integer.parseInt(request.getParameter("riot_capDoRiot")));
                    riot.setSoTuongLMHT(Integer.parseInt(request.getParameter("riot_soTuongLMHT")));
                    riot.setSoTrangPhucLMHT(Integer.parseInt(request.getParameter("riot_soTrangPhucLMHT")));
                    riot.setHangRankLMHT(request.getParameter("riot_hangRankLMHT"));
                    riot.setSoThuCungTFT(Integer.parseInt(request.getParameter("riot_soThuCungTFT")));
                    riot.setSoSanDauTFT(Integer.parseInt(request.getParameter("riot_soSanDauTFT")));
                    // (Thêm các trường khác nếu có)

                    if (maTaiKhoan == null) {
                        taiKhoanIdToUse = riotDAO.insert(riot, conn);
                    } else {
                        riot.setMaTaiKhoan(maTaiKhoan);
                        riotDAO.update(riot, conn);
                        taiKhoanIdToUse = maTaiKhoan;
                    }
                    break;

                default:
                    throw new SQLException("Mã danh mục không hợp lệ: " + maDanhMuc);
            }

            // 6. Xử lý lưu các Ảnh Chi Tiết
            for (String path : newAnhPhuPaths) {
                AnhTaiKhoan anh = new AnhTaiKhoan();
                anh.setMaTaiKhoan(taiKhoanIdToUse);
                anh.setDuongDanAnh(path);
                anhTaiKhoanDAO.insert(anh, conn);
            }

            // 7. Mọi thứ OK
            conn.commit();

        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                conn.rollback(); // Hủy bỏ tất cả thay đổi DB
            }
            // Hủy bỏ các file đã upload
            deleteFile(newAnhChinhPath);
            for (String path : newAnhPhuPaths) {
                deleteFile(path);
            }

            // Gửi lỗi về form
            request.setAttribute("errorMessage", "Lỗi khi lưu tài khoản: " + e.getMessage());
            // Tải lại dữ liệu cho form để người dùng sửa
            if (request.getParameter("maTaiKhoan") != null) {
                showEditForm(request, response);
            } else {
                showNewForm(request, response);
            }
            return; // Dừng lại, không chuyển hướng

        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/products/game");
    }

    private String saveFile(Part filePart) throws IOException {
        String originalFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        String fileExtension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            fileExtension = originalFileName.substring(i);
        }
        String newFileName = UUID.randomUUID().toString() + fileExtension;

        String filePath = EXTERNAL_UPLOAD_PATH + File.separator + newFileName;
        filePart.write(filePath);

        return "uploads/" + newFileName; // Đường dẫn lưu CSDL
    }

    private void deleteFile(String dbPath) {
        if (dbPath == null || dbPath.isEmpty() || !dbPath.startsWith("uploads/")) {
            return; // Chỉ xóa file trong thư mục uploads
        }

        try {
            String fileName = Paths.get(dbPath).getFileName().toString();
            String absolutePath = EXTERNAL_UPLOAD_PATH + File.separator + fileName;

            File fileToDelete = new File(absolutePath);
            if (fileToDelete.exists()) {
                fileToDelete.delete();
            }
        } catch (Exception e) {
            Logger.getLogger(AdminGameAccountServlet.class.getName()).log(Level.WARNING, "Không thể xóa file: " + dbPath, e);
        }
    }
}
