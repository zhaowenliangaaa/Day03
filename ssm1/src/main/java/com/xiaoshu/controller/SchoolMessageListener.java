package com.xiaoshu.controller;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xiaoshu.entity.School;

public class SchoolMessageListener implements MessageListener {

	@Autowired
	private RedisTemplate redisTemplate;
	
	
	public void onMessage(Message message) {
		//将message转化为TextMessage
		TextMessage textMessage = (TextMessage) message;
		try {
			String text = textMessage.getText();
			School school = JSON.parseObject(text, School.class);
			System.out.println("mq接收到的消息内容为："+school);
			redisTemplate.boundHashOps("schools").put("school", school);
			System.out.println("存入redis的信息数据为："+school);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	

}
