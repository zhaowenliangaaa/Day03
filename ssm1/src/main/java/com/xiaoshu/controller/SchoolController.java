package com.xiaoshu.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.xiaoshu.config.util.ConfigUtil;
import com.xiaoshu.dao.CityMapper;
import com.xiaoshu.entity.Attachment;
import com.xiaoshu.entity.City;
import com.xiaoshu.entity.Log;
import com.xiaoshu.entity.Operation;
import com.xiaoshu.entity.Role;
import com.xiaoshu.entity.School;
import com.xiaoshu.entity.User;
import com.xiaoshu.service.OperationService;
import com.xiaoshu.service.RoleService;
import com.xiaoshu.service.SchoolService;
import com.xiaoshu.service.UserService;
import com.xiaoshu.util.StringUtil;
import com.xiaoshu.util.TimeUtil;
import com.xiaoshu.util.WriterUtil;

@Controller
@RequestMapping("school")
public class SchoolController extends LogController{
	static Logger logger = Logger.getLogger(SchoolController.class);

	@Autowired
	private UserService userService;
	
	@Autowired
	private RoleService roleService ;
	
	@Autowired
	private OperationService operationService;
	
	@Autowired
	private SchoolService schoolService;
	@RequestMapping("schoolIndex")
	public String index(HttpServletRequest request,Integer menuid) throws Exception{
		List<Role> roleList = roleService.findRole(new Role());
		List<Operation> operationList = operationService.findOperationIdsByMenuid(menuid);
		//查询所有城市
		List<City> clist = schoolService.findCityList();
		request.setAttribute("clist", clist);
		request.setAttribute("operationList", operationList);
		request.setAttribute("roleList", roleList);
		return "school";
	}
	
	
	@RequestMapping(value="schoolList",method=RequestMethod.POST)
	public void userList(School school,HttpServletRequest request,HttpServletResponse response,String offset,String limit) throws Exception{
		try {
			String order = request.getParameter("order");
			String ordername = request.getParameter("ordername");
			Integer pageSize = StringUtil.isEmpty(limit)?ConfigUtil.getPageSize():Integer.parseInt(limit);
			Integer pageNum =  (Integer.parseInt(offset)/pageSize)+1;
			PageInfo<School> userList= schoolService.findUserPage(school,pageNum,pageSize,ordername,order);
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("total",userList.getTotal() );
			jsonObj.put("rows", userList.getList());
	        WriterUtil.write(response,jsonObj.toString());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("分校展示错误",e);
			throw e;
		}
	}
	
	
	// 新增或修改
	@RequestMapping("reserveSchool")
	public void reserveUser(HttpServletRequest request,School school,HttpServletResponse response){
		Integer userId = school.getId();
		JSONObject result=new JSONObject();
		try {
			if (userId != null) {   // userId不为空 说明是修改
				School userName = schoolService.existUserWithUserName(school.getSchoolname());
				/*if(userName != null && userName.getId().compareTo(userId)==0){*/
				if(userName == null ){
					school.setId(userId);
					schoolService.updateUser(school);
					result.put("success", true);
				}else{
					result.put("success", true);
					result.put("errorMsg", "该学校名称被使用");
				}
				
			}else {   // 添加
				if(schoolService.existUserWithUserName(school.getSchoolname())==null){  // 没有重复可以添加
					schoolService.addUser(school);
					result.put("success", true);
				} else {
					result.put("success", true);
					result.put("errorMsg", "该学校名称被使用");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("保存用户信息错误",e);
			result.put("success", true);
			result.put("errorMsg", "对不起，操作失败");
		}
		WriterUtil.write(response, result.toString());
	}
	
	
	@RequestMapping("deleteSchool")
	public void deleteSchool(HttpServletRequest request,HttpServletResponse response){
		JSONObject result=new JSONObject();
		try {
			String[] ids=request.getParameter("ids").split(",");
			for (String id : ids) {
				schoolService.deleteSchool(Integer.parseInt(id));
			}
			result.put("success", true);
			result.put("delNums", ids.length);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("删除用户信息错误",e);
			result.put("errorMsg", "对不起，删除失败");
		}
		WriterUtil.write(response, result.toString());
	}
	//报表方法
	@RequestMapping("getEcharts")
	public void getEcharts(HttpServletRequest request,HttpServletResponse response){
		JSONObject result=new JSONObject();
		try {
			List<School> slist = schoolService.getEchart();
			result.put("slist", slist);
			result.put("success", true);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("删除用户信息错误",e);
			result.put("errorMsg", "对不起，删除失败");
		}
		WriterUtil.write(response, result.toString());
	}
	/**
	 * 备份
	 */
	@RequestMapping("exportSchool")
	public void exportSchool(HttpServletRequest request,HttpServletResponse response){
		JSONObject result = new JSONObject();
		try {
			String time = TimeUtil.formatTime(new Date(), "yyyyMMddHHmmss");
		    String excelName = "手动备份"+time;
			School school =  new School();
			List<School> list = schoolService.findPage(school);
			String[] handers = {"序号","操作人","IP地址","操作时间","操作模块","操作类型","详情"};
			// 1导入硬盘
			ExportExcelToDisk(request,handers,list, excelName,response);
			result.put("success", true);
		} catch (Exception e) {
			e.printStackTrace();
			result.put("", "对不起，备份失败");
		}
		//WriterUtil.write(response, result.toString());
	}
	
	
	
	// 导出到硬盘
	@SuppressWarnings("resource")
	private void ExportExcelToDisk(HttpServletRequest request,
			String[] handers, List<School> list, String excleName,HttpServletResponse response) throws Exception {
		
		try {
			HSSFWorkbook wb = new HSSFWorkbook();//创建工作簿
			HSSFSheet sheet = wb.createSheet("操作记录备份");//第一个sheet
			HSSFRow rowFirst = sheet.createRow(0);//第一个sheet第一行为标题
			rowFirst.setHeight((short) 500);
			for (int i = 0; i < handers.length; i++) {
				sheet.setColumnWidth((short) i, (short) 4000);// 设置列宽
			}
			//写标题了
			for (int i = 0; i < handers.length; i++) {
			    //获取第一行的每一个单元格
			    HSSFCell cell = rowFirst.createCell(i);
			    //往单元格里面写入值
			    cell.setCellValue(handers[i]);
			}
			for (int i = 0;i < list.size(); i++) {
			    //获取list里面存在是数据集对象
			    School school = list.get(i);
			    //创建数据行
			    HSSFRow row = sheet.createRow(i+1);
			    //设置对应单元格的值
			    row.setHeight((short)400);   // 设置每行的高度
			    //"序号","操作人","IP地址","操作时间","操作模块","操作类型","详情"
			    row.createCell(0).setCellValue(i+1);
			    row.createCell(1).setCellValue(school.getSchoolname());
			    row.createCell(2).setCellValue(school.getAreaname());
			    row.createCell(3).setCellValue(school.getPhone());
			    row.createCell(4).setCellValue(school.getAddress());
			    row.createCell(5).setCellValue(school.getStatus());
			    row.createCell(6).setCellValue(TimeUtil.formatTime(school.getCreatetime(), "yyyy-MM-dd"));
			}
			//写出文件（path为文件路径含文件名）
			response.setHeader("Content-Disposition", "attachment;filename="+URLEncoder.encode("商品列表.xls", "UTF-8"));
			response.setHeader("Connection", "close");
			response.setHeader("Content-Type", "application/octet-stream");
	        wb.write(response.getOutputStream());
			wb.close();
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
	}
	//导入
	@RequestMapping("importSchool")
	public void importSchool(MultipartFile importFile,HttpServletRequest request,HttpServletResponse response){
		JSONObject result=new JSONObject();
		try {
			//获取文件信息
			HSSFWorkbook wb = new HSSFWorkbook(importFile.getInputStream());
			//解析文件,获取sheet页
			HSSFSheet sheetAt = wb.getSheetAt(0);
			//获取最后一行的行数
			int lastRowNum = sheetAt.getLastRowNum();
			//循环行数，获取每一个行对象
			for (int i = 1; i <= lastRowNum; i++) {
				//获取每一行的对象
				HSSFRow row = sheetAt.getRow(i);
				//获取单元格内的数据 分校名称 所在城市 联系方式 详细地址 分校状态 建校时间
				String schoolname = row.getCell(0).toString();
				String cname = row.getCell(1).toString();
				Double dou = row.getCell(2).getNumericCellValue();
				String phone = String.valueOf(dou);
				String address = row.getCell(3).toString();
				String status = row.getCell(4).toString();
				Date createtime = row.getCell(5).getDateCellValue();
				//根据城市名称查询城市id
				Integer areaid = findCidByCname(cname);
				//封装school对象
				School school = new School();
				school.setAddress(address);
				school.setAreaid(areaid);
				school.setCreatetime(createtime);
				school.setPhone(phone);
				school.setSchoolname(schoolname);
				school.setStatus(status);
				//添加
				schoolService.addUser(school);
			}
			result.put("success", true);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("删除用户信息错误",e);
			result.put("errorMsg", "对不起，删除失败");
		}
		WriterUtil.write(response, result.toString());
	}
	@Autowired
	private CityMapper cityMapper;
	public Integer findCidByCname(String cname) {
		City city = new  City();
		city.setAreaname(cname);
		City one = cityMapper.selectOne(city);
		if(one==null){
			cityMapper.insertCity(city);
			one = city;
		}
		return one.getId();
	}


	@RequestMapping("editPassword")
	public void editPassword(HttpServletRequest request,HttpServletResponse response){
		JSONObject result=new JSONObject();
		String oldpassword = request.getParameter("oldpassword");
		String newpassword = request.getParameter("newpassword");
		HttpSession session = request.getSession();
		User currentUser = (User) session.getAttribute("currentUser");
		if(currentUser.getPassword().equals(oldpassword)){
			User user = new User();
			user.setUserid(currentUser.getUserid());
			user.setPassword(newpassword);
			try {
				userService.updateUser(user);
				currentUser.setPassword(newpassword);
				session.removeAttribute("currentUser"); 
				session.setAttribute("currentUser", currentUser);
				result.put("success", true);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("修改密码错误",e);
				result.put("errorMsg", "对不起，修改密码失败");
			}
		}else{
			logger.error(currentUser.getUsername()+"修改密码时原密码输入错误！");
			result.put("errorMsg", "对不起，原密码输入错误！");
		}
		WriterUtil.write(response, result.toString());
	}
}
