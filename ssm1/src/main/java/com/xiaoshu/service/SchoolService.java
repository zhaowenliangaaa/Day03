package com.xiaoshu.service;

import java.util.Date;
import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.util.StringUtil;
import com.xiaoshu.dao.CityMapper;
import com.xiaoshu.dao.SchoolMapper;
import com.xiaoshu.dao.UserMapper;
import com.xiaoshu.entity.City;
import com.xiaoshu.entity.School;
import com.xiaoshu.entity.SchoolExample;
import com.xiaoshu.entity.SchoolExample.Criteria;


@Service
public class SchoolService {

	@Autowired
	UserMapper userMapper;
	@Autowired
	private SchoolMapper schoolMapper;
	
	@Autowired
	private JmsTemplate jmsTemplate;
	@Autowired
	private Destination queueTextDestination;
	// 新增
	public void addUser(final School t) throws Exception {
		t.setCreatetime(new Date());
		schoolMapper.insert(t);
		jmsTemplate.send(queueTextDestination, new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				String json = JSON.toJSONString(t);
				return session.createTextMessage(json);
			}
		});
	};
	// 修改
	public void updateUser(School t) throws Exception {
		schoolMapper.updateByPrimaryKeySelective(t);
	};
	// 删除
	public void deleteSchool(Integer id) throws Exception {
		schoolMapper.deleteByPrimaryKey(id);
	};
	// 通过用户名判断是否存在，（新增时不能重名）
	public School existUserWithUserName(String userName) throws Exception {
		SchoolExample example = new SchoolExample();
		Criteria criteria = example.createCriteria();
		criteria.andSchoolnameEqualTo(userName);
		List<School> userList = schoolMapper.selectByExample(example);
		return userList.isEmpty()?null:userList.get(0);
	};

	public PageInfo<School> findUserPage(School school, int pageNum, int pageSize, String ordername, String order) {
		PageHelper.startPage(pageNum, pageSize);
		ordername = StringUtil.isNotEmpty(ordername)?ordername:"userid";
		order = StringUtil.isNotEmpty(order)?order:"desc";
		
		List<School> userList = schoolMapper.findPage(school);
		PageInfo<School> pageInfo = new PageInfo<School>(userList);
		return pageInfo;
	}
	@Autowired
	private CityMapper cityMapper;
	public List<City> findCityList() {
		return cityMapper.selectAll();
	}
	public List<School> findPage(School school) {
		return schoolMapper.findPage(school);
	}
	public List<School> getEchart() {
		// TODO Auto-generated method stub
		return schoolMapper.getEchart();
	}
	
	


}
