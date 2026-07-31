import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import DetectScreen from './src/screens/DetectScreen';
import ReportScreen from './src/screens/ReportScreen';
import StatusScreen from './src/screens/StatusScreen';

const Tab = createBottomTabNavigator();

export default function App() {
  return (
    <NavigationContainer>
      <Tab.Navigator screenOptions={{ headerTitleAlign: 'center' }}>
        <Tab.Screen name="Detect" component={DetectScreen} />
        <Tab.Screen name="Report" component={ReportScreen} />
        <Tab.Screen name="My Reports" component={StatusScreen} />
      </Tab.Navigator>
    </NavigationContainer>
  );
}
