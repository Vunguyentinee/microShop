package com.microshop.controller;

import com.microshop.dao.DonHangDAO;
import com.microshop.dao.DonHangSlotSteamDAO;
import com.microshop.dao.NguoiDungDAO;
import com.microshop.dao.TaiKhoanDAO;
import com.microshop.model.DonHang;
import com.microshop.model.DonHangSlotSteam;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

// 
@WebServlet(name = "AdminDashboardServlet", urlPatterns = {"/admin/", "/admin/dashboard"})
public class AdminDashboardServlet extends HttpServlet {

    private TaiKhoanDAO taiKhoanDAO;
    private NguoiDungDAO nguoiDungDAO;
    private DonHangDAO donHangDAO;
    private DonHangSlotSteamDAO donHangSlotSteamDAO;

    @Override
    public void init() {
        taiKhoanDAO = new TaiKhoanDAO();
        nguoiDungDAO = new NguoiDungDAO();
        donHangDAO = new DonHangDAO();
        donHangSlotSteamDAO = new DonHangSlotSteamDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            int totalTaiKhoan = taiKhoanDAO.getTotalCount();
            int totalNguoiDung = nguoiDungDAO.getTotalCount();
            int totalDonHang = donHangDAO.getTotalCount() + donHangSlotSteamDAO.getTotalCount();
            int totalDonHangCho = donHangDAO.getCountByTrangThai("CHO_THANH_TOAN")
                    + donHangSlotSteamDAO.getCountByTrangThai("CHO_THANH_TOAN");

            request.setAttribute("totalTaiKhoan", totalTaiKhoan);
            request.setAttribute("totalNguoiDung", totalNguoiDung);
            request.setAttribute("totalDonHang", totalDonHang);
            request.setAttribute("totalDonHangCho", totalDonHangCho);

            int page = 1;
            int recordsPerPage = 10;
            if (request.getParameter("page") != null) {
                try {
                    page = Integer.parseInt(request.getParameter("page"));
                } catch (NumberFormatException e) {
                    page = 1;
                }
            }

            List<DonHang> listDonHang = donHangDAO.getAllPaginated(page, recordsPerPage);
            int recCount = donHangDAO.getTotalCount();
            int pageCount = (int) Math.ceil(recCount * 1.0 / recordsPerPage);

            request.setAttribute("listDonHang", listDonHang);
            request.setAttribute("pageCount", pageCount);
            request.setAttribute("currentPage", page);

            // Đúng ra phải dùng 2 biến page cho 2 loại ( cóng lắm rồi không sửa đâu )
            List<DonHangSlotSteam> listDonHangSteam = donHangSlotSteamDAO.getAllPaginated(page, recordsPerPage);
            int recCountSteam = donHangSlotSteamDAO.getTotalCount();
            int pageCountSteam = (int) Math.ceil(recCountSteam * 1.0 / recordsPerPage);

            request.setAttribute("listDonHangSteam", listDonHangSteam);
            request.setAttribute("pageCountSteam", pageCountSteam);

            RequestDispatcher dispatcher = request.getRequestDispatcher("/admin/dashboard.jsp");
            dispatcher.forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/home");
        }
    }
}
