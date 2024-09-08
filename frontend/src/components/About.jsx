import 'animate.css';
import { useEffect, useRef } from 'react';

export const About = () => {
    const sectionRefs = useRef([]);

    useEffect(() => {
        //create intersection observer
        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach((entry) => {
                    if(entry.isIntersecting) {
                        //add animation class when section is in view
                        entry.target.classList.add('animate__animated', 'animate__fadeInUp');
                    }
                });
            },
            { threshold: 0.1 } 
        );

        //observe each section
        sectionRefs.current.forEach((section) => {
            if(section) observer.observe(section);
        });

        return () => {
            //cleanup observer on component unmount
            sectionRefs.current.forEach((section) => {
                if (section) observer.unobserve(section);
            });
        };
    }, []);

    return (
        <div className="flex flex-col items-center mt-16 text-gray-200 text-center font-roboto">
            <h1 className="text-3xl font-roboto" ref={(el) => sectionRefs.current[0] = el}>What is Shiru?</h1>
                <p className="mt-8" ref={(el) => sectionRefs.current[1] = el}>
                    Shiru is a web app that scraps manga from certain websites and uses the OpenAI API to
                    <br></br>
                    determine the JLPT level associated with said manga!
                    <br></br>
                    This site was made using Java, React, TypeScript, Tailwind, and SpringBoot.
                    <br></br>
                    You can check out the source code <a href="https://github.com/Josh-Harrison1600/Shiru-Scraper" target='_blank' className='text-blue-500 hover:text-white duration-300'>here</a>.
                </p>
            
            <h2 className="text-3xl font-roboto mt-10" ref={(el) => sectionRefs.current[2] = el}>What is JLPT?</h2>
            <p className="mt-8" ref={(el) => sectionRefs.current[3] = el}>
                    The Japanese Language Proficiency Test (JLPT) is an exam issued by the Japan Foundation 
                    <br></br>
                    for learners of the language to assess their skills with vocabulary, grammar, listening 
                    <br></br>
                    and reading. The exams range from 5 levels, N5 - N1. N5 being the easiest and N1 being the hardest. 
                    <br></br>
                    I used this as my method of classifying difficulty as most Japanese learners are familiar with 
                    <br></br>
                    JLPT exams and where their skill level is in relation to them.
                </p>

            <h3 className="text-3xl font-roboto mt-10" ref={(el) => sectionRefs.current[4] = el}>Is Shiru Accurate?</h3>
            <p className="mt-8" ref={(el) => sectionRefs.current[5] = el}>
                Kinda...
                <br></br>
                This site uses the OpenAI API (ChatGPT) in order to determine the difficulty of these manga books so the 
                <br></br>
                placement of these manga in their respected categories should be taken with a grain of salt as ChatGPT
                <br></br>
                is known to hallucinate occasionally. However if a manga is placed incorrectly it shouldn't be more then
                <br></br>
                1 level. For example you shouldn't see any N2 manga in the N5 Category, but you might see some N3 manga 
                <br></br>
                in the N4 or N2 category occasionally for example.
            
            </p>
        </div>
    );
}

export default About;